# Status do Orçamento — Expiração Automática por Data Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Solicitado/Enviado orçamentos whose validity date has passed should show as "Expirado" everywhere the system counts, filters, or displays status for analysis — without ever writing that to the database.

**Architecture:** Extract the existing (private, PDF-only) due-date calculation into a shared utility, add a computed `getStatusExibicao()` getter to the `Orcamento` entity that the existing entity-as-response-body pattern serializes for free, then migrate every frontend read site that aggregates or displays status — except the edit form, which keeps reading/writing the raw stored value.

**Tech Stack:** Spring Boot 3 / Java 17, JUnit 5 + Mockito + AssertJ (backend); React 18 + TypeScript (frontend)

**Repositories:** `marmoraria-orcamentos-api` (Tasks 1–2) and `marmoraria-orcamentos-web` (Task 3). Work on a new branch in each — this change is purely additive on the wire (see the compatibility note in the document-generation plan for why that matters), so the two repos' branches can be merged in either order.

**Spec:** `docs/superpowers/specs/2026-08-15-status-orcamento-expirado-design.md`

**Note on the existing test suite:** `OrcamentoServiceTest.java` currently has several pre-existing, unrelated failures (confirmed by running `./mvnw -Dtest=OrcamentoServiceTest test` before starting this plan — 10/10 tests fail with NPEs, because the test fixtures and the current `OrcamentoService` implementation have drifted apart). This is tracked separately and is **not** part of this plan. When Task 2 asks you to run this file's tests, scope the command to the one new test method so the unrelated failures don't obscure the result.

---

## File Structure

- Create: `src/main/java/com/marmoraria/orcamentos/util/OrcamentoDatas.java` — shared due-date calculation.
- Modify: `src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java` — use the shared util instead of its own private copy.
- Modify: `src/main/java/com/marmoraria/orcamentos/entity/Orcamento.java` — add `getStatusExibicao()`.
- Create: `src/test/java/com/marmoraria/orcamentos/entity/OrcamentoTest.java` — tests for the getter.
- Modify: `src/types/api.types.ts` (in `marmoraria-orcamentos-web`) — add `statusExibicao` to the `Orcamento` interface.
- Modify: `src/pages/DashboardPage/DashboardPage.tsx`, `src/pages/OrcamentoDetailPage/OrcamentoDetailPage.tsx`, `src/pages/OrcamentosPage/OrcamentosPage.tsx` (all in `marmoraria-orcamentos-web`) — read `statusExibicao` instead of `statusOrcamento` for display/aggregation.

All backend paths below are relative to the `marmoraria-orcamentos-api` project root; all frontend paths are relative to the `marmoraria-orcamentos-web` project root.

---

### Task 1: Extract the shared due-date calculation

**Files:**
- Create: `src/main/java/com/marmoraria/orcamentos/util/OrcamentoDatas.java`
- Modify: `src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java`

This is a pure refactor — same behavior, moved so Task 2 can reuse it without duplicating the logic. No new test; verified by compiling and by the fact that Task 2's tests exercise the same calculation through the new home.

- [ ] **Step 1: Create the shared utility**

**Note (updated after Task 1's code review):** this lives in the `entity` package, not `util` — putting a class that both imports `Orcamento` and is imported back by `Orcamento` (Task 2) in a separate `util` package creates a circular package dependency. Same package on both sides avoids it entirely, with no behavior difference.

Create `src/main/java/com/marmoraria/orcamentos/entity/OrcamentoDatas.java`:

```java
package com.marmoraria.orcamentos.entity;

import java.time.LocalDate;

public final class OrcamentoDatas {

    private OrcamentoDatas() {
    }

    public static LocalDate vencimento(Orcamento orcamento) {
        if (orcamento.getDataEmissao() != null && orcamento.getValidadeDias() != null) {
            return orcamento.getDataEmissao().plusDays(orcamento.getValidadeDias());
        }
        return orcamento.getDataValidade();
    }
}
```

This is an exact copy of the private `dataVencimento(Orcamento)` method already in `OrcamentoDocumentoService.java` — just moved and made a public static utility so it can be shared.

- [ ] **Step 2: Point `OrcamentoDocumentoService` at the shared utility**

In `src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java`:

Add the import, alongside the other `com.marmoraria.orcamentos.entity.*` imports near the top:

```java
import com.marmoraria.orcamentos.entity.OrcamentoDatas;
```

There are three call sites of the private method, each a line like `dataVencimento(orcamento)`. Replace each with `OrcamentoDatas.vencimento(orcamento)`:

1. Inside `paginaResumo(Orcamento orcamento)`:
   ```java
   LocalDate vencimento = dataVencimento(orcamento);
   ```
   becomes
   ```java
   LocalDate vencimento = OrcamentoDatas.vencimento(orcamento);
   ```

2. Inside `paginaIdentificacao(Orcamento orcamento, String responsavelTecnico, OpcoesGeracaoRequest opcoes)`: same replacement (identical line).

3. Inside `cabecalho(Orcamento orcamento)`:
   ```java
   dados.put("DATA_VENCIMENTO", esc(formatarData(dataVencimento(orcamento))));
   ```
   becomes
   ```java
   dados.put("DATA_VENCIMENTO", esc(formatarData(OrcamentoDatas.vencimento(orcamento))));
   ```

Then delete the now-unused private method (near the bottom of the class, right before `numeroOrcamento`):

```java
    private LocalDate dataVencimento(Orcamento orcamento) {
        if (orcamento.getDataEmissao() != null && orcamento.getValidadeDias() != null) {
            return orcamento.getDataEmissao().plusDays(orcamento.getValidadeDias());
        }
        return orcamento.getDataValidade();
    }
```

- [ ] **Step 3: Verify it compiles**

```bash
./mvnw -q compile
```

Expected: no output, exit code 0. (No dedicated test exists for `OrcamentoDocumentoService` yet — the document-generation plan adds one; this step's only job is to confirm the refactor didn't break a reference.)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/marmoraria/orcamentos/util/OrcamentoDatas.java src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java
git commit -m "refactor: extract due-date calculation into shared OrcamentoDatas util"
```

---

### Task 2: `Orcamento.getStatusExibicao()` (TDD)

**Files:**
- Create: `src/test/java/com/marmoraria/orcamentos/entity/OrcamentoTest.java`
- Modify: `src/main/java/com/marmoraria/orcamentos/entity/Orcamento.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/marmoraria/orcamentos/entity/OrcamentoTest.java`:

```java
package com.marmoraria.orcamentos.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OrcamentoTest {

    private Orcamento orcamentoComDias(StatusOrcamento status, int diasDesdeEmissao, int validadeDias) {
        Orcamento orcamento = new Orcamento();
        orcamento.setStatusOrcamento(status);
        orcamento.setDataEmissao(LocalDate.now().minusDays(diasDesdeEmissao));
        orcamento.setValidadeDias(validadeDias);
        return orcamento;
    }

    @Test
    void solicitadoVencidoViraExpiradoNaExibicao() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.SOLICITADO, 20, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.EXPIRADO);
        assertThat(orcamento.getStatusOrcamento()).isEqualTo(StatusOrcamento.SOLICITADO);
    }

    @Test
    void enviadoDentroDaValidadeContinuaEnviado() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.ENVIADO, 5, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.ENVIADO);
    }

    @Test
    void aprovadoVencidoNaoViraExpirado() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.APROVADO, 20, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.APROVADO);
    }

    @Test
    void rejeitadoVencidoNaoViraExpirado() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.REJEITADO, 20, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.REJEITADO);
    }

    @Test
    void rascunhoVencidoNaoViraExpirado() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.RASCUNHO, 20, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.RASCUNHO);
    }

    @Test
    void semDataEmissaoNaoQuebraOCalculo() {
        Orcamento orcamento = new Orcamento();
        orcamento.setStatusOrcamento(StatusOrcamento.SOLICITADO);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.SOLICITADO);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./mvnw -q -Dtest=OrcamentoTest test
```

Expected: FAIL to compile — `cannot find symbol: method getStatusExibicao()` (the method doesn't exist on `Orcamento` yet).

- [ ] **Step 3: Implement the getter**

In `src/main/java/com/marmoraria/orcamentos/entity/Orcamento.java`:

No new import needed — `OrcamentoDatas` now lives in the same `entity` package as `Orcamento` (see the updated Task 1 above), so it's directly accessible.

Add the method at the end of the class, right after the existing `statusOrcamento` field (this is the last field in the class):

```java
    @NotNull(message = "Status do orcamento e obrigatorio")
    @Enumerated(EnumType.STRING)
    private StatusOrcamento statusOrcamento;

    public StatusOrcamento getStatusExibicao() {
        boolean elegivelParaExpirar = statusOrcamento == StatusOrcamento.SOLICITADO
                || statusOrcamento == StatusOrcamento.ENVIADO;
        LocalDate vencimento = OrcamentoDatas.vencimento(this);
        if (elegivelParaExpirar && vencimento != null && vencimento.isBefore(LocalDate.now())) {
            return StatusOrcamento.EXPIRADO;
        }
        return statusOrcamento;
    }
}
```

(`Orcamento` already imports `java.time.LocalDate` for the `dataValidade`/`dataEmissao` fields, so no new import is needed for that type.)

No `@Transient` annotation is needed: the entity uses field-based access (`@Id` is on a field), so a getter with no backing field is invisible to Hibernate automatically. Jackson still picks it up and serializes it as `"statusExibicao"` in every JSON response, since the controllers return `Orcamento` directly.

- [ ] **Step 4: Run the test to verify it passes**

```bash
./mvnw -q -Dtest=OrcamentoTest test
```

Expected: PASS — all 6 tests green.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/marmoraria/orcamentos/entity/Orcamento.java src/test/java/com/marmoraria/orcamentos/entity/OrcamentoTest.java
git commit -m "feat: add computed statusExibicao (Solicitado/Enviado expire on due date)"
```

---

### Task 3: Frontend — read `statusExibicao` everywhere it matters

**Files:**
- Modify: `src/types/api.types.ts`
- Modify: `src/pages/DashboardPage/DashboardPage.tsx`
- Modify: `src/pages/OrcamentoDetailPage/OrcamentoDetailPage.tsx`
- Modify: `src/pages/OrcamentosPage/OrcamentosPage.tsx`

No automated test coverage for this task (see the auth plan for why this repo has no component-testing setup) — each step below is a direct edit, verified together at the end by Step 6's manual pass.

- [ ] **Step 1: Add the new field to the `Orcamento` type**

In `src/types/api.types.ts`, the `Orcamento` interface currently starts:

```ts
export interface Orcamento {
  id: number;
  numero: string;
  statusOrcamento: OrcamentoStatus;
  cliente: Cliente;
```

Change it to:

```ts
export interface Orcamento {
  id: number;
  numero: string;
  /** Valor bruto gravado no banco — usar apenas no formulário de edição. */
  statusOrcamento: OrcamentoStatus;
  /** Status calculado (Solicitado/Enviado vencidos viram Expirado) — usar em badges, contadores e filtros. */
  statusExibicao: OrcamentoStatus;
  cliente: Cliente;
```

- [ ] **Step 2: Dashboard — swap the aggregation and display reads**

In `src/pages/DashboardPage/DashboardPage.tsx`, inside `DashboardPage()`, these five reads of `.statusOrcamento`:

```ts
  const pendentes = orcamentos.filter((o) => ['SOLICITADO', 'ENVIADO'].includes(o.statusOrcamento)).length;
  const aprovados = orcamentos.filter((o) => o.statusOrcamento === 'APROVADO').length;
  const valorAprovado = orcamentos
    .filter((o) => o.statusOrcamento === 'APROVADO')
    .reduce((s, o) => s + (o.financeiro?.totalFinal ?? o.valorTotal ?? 0), 0);

  const statusCounts: Record<string, number> = {};
  orcamentos.forEach((o) => {
    statusCounts[o.statusOrcamento] = (statusCounts[o.statusOrcamento] ?? 0) + 1;
  });
```

become:

```ts
  const pendentes = orcamentos.filter((o) => ['SOLICITADO', 'ENVIADO'].includes(o.statusExibicao)).length;
  const aprovados = orcamentos.filter((o) => o.statusExibicao === 'APROVADO').length;
  const valorAprovado = orcamentos
    .filter((o) => o.statusExibicao === 'APROVADO')
    .reduce((s, o) => s + (o.financeiro?.totalFinal ?? o.valorTotal ?? 0), 0);

  const statusCounts: Record<string, number> = {};
  orcamentos.forEach((o) => {
    statusCounts[o.statusExibicao] = (statusCounts[o.statusExibicao] ?? 0) + 1;
  });
```

And further down, inside the `lineData` computation:

```ts
    count: orcamentos.filter(
      (o) => o.statusOrcamento === 'APROVADO' && o.dataEmissao?.startsWith(month)
    ).length,
```

becomes:

```ts
    count: orcamentos.filter(
      (o) => o.statusExibicao === 'APROVADO' && o.dataEmissao?.startsWith(month)
    ).length,
```

- [ ] **Step 3: Detail page — badge**

In `src/pages/OrcamentoDetailPage/OrcamentoDetailPage.tsx`:

```tsx
              <Row label="Status" value={<StatusBadge status={orcamento.statusOrcamento} />} />
```

becomes:

```tsx
              <Row label="Status" value={<StatusBadge status={orcamento.statusExibicao} />} />
```

- [ ] **Step 4: Listing page — badge**

In `src/pages/OrcamentosPage/OrcamentosPage.tsx`:

```tsx
                  <td><StatusBadge status={o.statusOrcamento} /></td>
```

becomes:

```tsx
                  <td><StatusBadge status={o.statusExibicao} /></td>
```

- [ ] **Step 5: Leave the edit form alone**

Do **not** change `src/pages/OrcamentoFormPage/OrcamentoFormPage.tsx` — it reads and writes `statusOrcamento` (the raw, editable value) by design; that's the one place the distinction matters. Confirm it still reads `orc.statusOrcamento` (line ~100) and sends `statusOrcamento: status` (line ~169) unchanged.

- [ ] **Step 6: Manual verification**

1. Run `npm run dev` against a backend running Task 1–2 of this plan (or against a backend where those are already merged).
2. Create or edit a test orçamento: status `Solicitado`, `dataEmissao` at least 20 days in the past, `validadeDias` = 15.
3. Open `/orcamentos` (listing) and `/orcamentos/:id` (detail) — **expected:** both show an "Expirado" badge, not "Solicitado".
4. Open `/dashboard` — **expected:** this orçamento is not counted in "Pendentes", and shows up in the donut chart's "Outros" slice, not "Pendente".
5. Edit the same orçamento (`/orcamentos/:id/editar`) — **expected:** the status dropdown still shows "Solicitado" (the real stored value), not "Expirado".
6. Repeat with an `Aprovado` orçamento that also has an overdue date — **expected:** stays "Aprovado" everywhere, including the dashboard's "Aprovados" KPI and chart.

- [ ] **Step 7: Commit**

```bash
git add src/types/api.types.ts src/pages/DashboardPage/DashboardPage.tsx src/pages/OrcamentoDetailPage/OrcamentoDetailPage.tsx src/pages/OrcamentosPage/OrcamentosPage.tsx
git commit -m "feat: display computed statusExibicao in badges, counters and dashboard"
```

---

## Self-Review

**Spec coverage:** All three backend decisions (EXPIRADO not VENCIDO, `dataEmissao + validadeDias` not `dataValidade`, Solicitado/Enviado-only eligibility) are encoded directly in Task 2's implementation and tests. The full audit table from the spec (which of the 6 read sites change, which one — the edit form — stays raw) is Task 3, one step per row. "Fora de escopo" items (removing `VENCIDO`, persisting the computed status, populating `dataValidade`) have no corresponding task, intentionally.

**Placeholder scan:** No TBD/TODO; every step shows complete before/after code.

**Type consistency:** `getStatusExibicao()` returns `StatusOrcamento` (Java enum) in Task 2; the frontend's `statusExibicao: OrcamentoStatus` in Task 3 mirrors it as the equivalent TypeScript union. `OrcamentoDatas.vencimento(Orcamento)` signature in Task 1 matches exactly how it's called in Task 2's `Orcamento.getStatusExibicao()`.

**Verified against the real codebase before writing this plan:** Tasks 1–2's exact code (the `OrcamentoDatas` extraction, the `getStatusExibicao()` getter, and the six `OrcamentoTest` cases above) was implemented directly in the worktree and run with `./mvnw -Dtest=OrcamentoTest test` — all 6 passed — then reverted before writing this document, specifically because this session already found one existing test file whose assumptions didn't match the real service behavior, and a second surprise wasn't worth risking in a plan someone else will follow literally.
