# Ordem dos Itens no Orçamento Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Items appear in the generated document in the order they were added to the orçamento, not in whatever order Postgres happens to return them.

**Architecture:** Add a persisted `ordem` column to `item_orcamento` (mirroring the existing `ItemOrcamentoImagem` pattern), have `OrcamentoService` assign it from list position on every save, and let JPA's `@OrderBy` do the rest — the document template already just iterates the list it's given.

**Tech Stack:** Spring Boot 3 / Java 17, Flyway, JUnit 5 + Mockito + AssertJ

**Repository:** `marmoraria-orcamentos-api` only — no frontend changes.

**Spec:** `docs/superpowers/specs/2026-08-15-ordem-itens-orcamento-design.md`

**Note on the existing test suite:** `OrcamentoServiceTest.java` currently has several pre-existing, unrelated failures (10/10 tests fail on `main` today — confirmed by running `./mvnw -Dtest=OrcamentoServiceTest test`). This predates and is unrelated to this plan; it's tracked as a separate follow-up. Task 2 below scopes its test run to the one new method so those failures don't obscure the result.

**Suggested order:** implement this plan before the document-generation plan — the document-generation plan creates `OrcamentoDocumentoServiceTest.java` fresh if it doesn't exist yet, but this plan creates it first with one test in it, which is simpler than the reverse.

---

## File Structure

- Create: `src/main/resources/db/migration/V3__add_ordem_item_orcamento.sql`
- Modify: `src/main/java/com/marmoraria/orcamentos/entity/ItemOrcamento.java` — add `ordem` field.
- Modify: `src/main/java/com/marmoraria/orcamentos/entity/Orcamento.java` — add `@OrderBy` to `itemOrcamentoList`.
- Modify: `src/main/java/com/marmoraria/orcamentos/service/OrcamentoService.java` — assign `ordem` by list position.
- Modify: `src/test/java/com/marmoraria/orcamentos/service/OrcamentoServiceTest.java` — one new test.
- Create: `src/test/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoServiceTest.java` — one test confirming the template respects list order.

---

### Task 1: Migration and entity changes

**Files:**
- Create: `src/main/resources/db/migration/V3__add_ordem_item_orcamento.sql`
- Modify: `src/main/java/com/marmoraria/orcamentos/entity/ItemOrcamento.java`
- Modify: `src/main/java/com/marmoraria/orcamentos/entity/Orcamento.java`

- [ ] **Step 1: Write the migration**

Create `src/main/resources/db/migration/V3__add_ordem_item_orcamento.sql`:

```sql
ALTER TABLE item_orcamento ADD COLUMN ordem INTEGER;

UPDATE item_orcamento AS io
SET ordem = sub.rn
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY orcamento_id ORDER BY id) - 1 AS rn
    FROM item_orcamento
) AS sub
WHERE io.id = sub.id;
```

The column stays nullable — every item gets a value on the next save (Step 3 of Task 2), and Postgres already sorts `NULL` last in `ORDER BY ordem ASC` by default, so a never-touched row just sorts to the end instead of erroring.

- [ ] **Step 2: Add the field to `ItemOrcamento`**

In `src/main/java/com/marmoraria/orcamentos/entity/ItemOrcamento.java`, the class currently ends:

```java
    private BigDecimal valorDesconto;

    private BigDecimal valorTotal;
}
```

Change it to:

```java
    private BigDecimal valorDesconto;

    private BigDecimal valorTotal;

    private Integer ordem;
}
```

- [ ] **Step 3: Order the collection in `Orcamento`**

In `src/main/java/com/marmoraria/orcamentos/entity/Orcamento.java`, add the import next to the other `jakarta.persistence.*` imports:

```java
import jakarta.persistence.OrderBy;
```

And change:

```java
    @Valid
    @JsonManagedReference
    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemOrcamento> itemOrcamentoList;
```

to:

```java
    @Valid
    @JsonManagedReference
    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("ordem ASC, id ASC")
    private List<ItemOrcamento> itemOrcamentoList;
```

This is the exact same pattern already used on `ItemOrcamento.imagens` (`@OrderBy("ordem ASC, id ASC")`) — nothing new to the codebase, just applied one level up.

- [ ] **Step 4: Verify the migration applies cleanly**

This needs a real Postgres instance — Mockito-based unit tests don't start Flyway. If you have the `local` profile configured (see the project README: copy `application-example.properties` to `application-local.properties`, local Postgres database `marmoraria_orcamentos`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Expected in the startup log: a line showing Flyway migrating to version 3, e.g. `Migrating schema "public" to version "3 - add ordem item orcamento"`, then a normal successful startup with no errors. Stop the process (Ctrl+C) once confirmed.

If local Postgres isn't set up right now, at minimum run:

```bash
./mvnw -q compile
```

to confirm the entity changes compile, and apply/verify the migration before merging — Flyway will refuse to start the app at all against the real database if the SQL is malformed, so this can't silently ship broken.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V3__add_ordem_item_orcamento.sql src/main/java/com/marmoraria/orcamentos/entity/ItemOrcamento.java src/main/java/com/marmoraria/orcamentos/entity/Orcamento.java
git commit -m "feat: add ordem column to item_orcamento with backfill"
```

---

### Task 2: Assign `ordem` on save (TDD)

**Files:**
- Modify: `src/test/java/com/marmoraria/orcamentos/service/OrcamentoServiceTest.java`
- Modify: `src/main/java/com/marmoraria/orcamentos/service/OrcamentoService.java`

- [ ] **Step 1: Write the failing test**

In `src/test/java/com/marmoraria/orcamentos/service/OrcamentoServiceTest.java`, add this test right before the final closing `}` of the class (after `calcularValorTotal_validadeDiasInvalida_lancaExcecao`):

```java
    @Test
    void calcularValorTotalAtribuiOrdemPelaPosicaoNaLista() {
        ItemOrcamento primeiro = new ItemOrcamento();
        primeiro.setValorTotal(new BigDecimal("10.00"));

        ItemOrcamento segundo = new ItemOrcamento();
        segundo.setValorTotal(new BigDecimal("20.00"));

        orcamento.setItemOrcamentoList(List.of(primeiro, segundo));

        service.calcularValorTotal(orcamento);

        assertThat(primeiro.getOrdem()).isEqualTo(0);
        assertThat(segundo.getOrdem()).isEqualTo(1);
    }
```

No new imports needed — `ItemOrcamento`, `BigDecimal`, `List`, and `assertThat` are already imported by this file. Note this test builds its `ItemOrcamento` fixtures by setting `valorTotal` directly rather than using the file's existing `itemCom()` helper — `itemCom()` only sets `subtotal`/`precoUnitario`, and this class's `itemOrcamentoService.calcularValorTotal(...)` mock is stubbed to do nothing, so anything relying on that mock to populate `valorTotal` hits the same pre-existing NPE mentioned at the top of this plan. Setting `valorTotal` directly sidesteps it.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./mvnw -q -Dtest=OrcamentoServiceTest#calcularValorTotalAtribuiOrdemPelaPosicaoNaLista test
```

Expected: FAIL — `assertThat(primeiro.getOrdem()).isEqualTo(0)` fails because `getOrdem()` returns `null` (nothing sets it yet).

- [ ] **Step 3: Implement the assignment**

In `src/main/java/com/marmoraria/orcamentos/service/OrcamentoService.java`, inside `calcularValorTotal(Orcamento orcamento)`:

```java
        if (itens != null) {
            for (ItemOrcamento item : itens) {
                item.setOrcamento(orcamento);
                itemOrcamentoService.calcularValorTotal(item);
                totalItens = totalItens.add(item.getValorTotal());
            }
        }
```

becomes:

```java
        if (itens != null) {
            for (int i = 0; i < itens.size(); i++) {
                ItemOrcamento item = itens.get(i);
                item.setOrcamento(orcamento);
                item.setOrdem(i);
                itemOrcamentoService.calcularValorTotal(item);
                totalItens = totalItens.add(item.getValorTotal());
            }
        }
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./mvnw -q -Dtest=OrcamentoServiceTest#calcularValorTotalAtribuiOrdemPelaPosicaoNaLista test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/marmoraria/orcamentos/service/OrcamentoService.java src/test/java/com/marmoraria/orcamentos/service/OrcamentoServiceTest.java
git commit -m "feat: assign item ordem by position on every orcamento save"
```

---

### Task 3: Confirm the document respects the order (TDD)

**Files:**
- Create: `src/test/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoServiceTest.java`

`OrcamentoDocumentoService` never re-sorts the item list itself — it trusts whatever order it's handed, which is exactly why Tasks 1–2 (persisting and applying `ordem` at the JPA level) are what actually fixes the bug. This test is a regression guard at the template layer: it confirms `paginaItens()` keeps preserving list order, so a future change in that method can't silently start reordering things again. It does not exercise Hibernate/`@OrderBy` itself (that would need a `@DataJpaTest` against a real database, which nothing else in this codebase uses either — the existing `ItemOrcamentoImagem.imagens` collection relies on the same annotation with no dedicated JPA-level test).

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoServiceTest.java`:

```java
package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.Cliente;
import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.entity.StatusOrcamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcamentoDocumentoServiceTest {

    @Mock
    private OrcamentoService orcamentoService;

    @InjectMocks
    private OrcamentoDocumentoService service;

    private Orcamento orcamento;

    @BeforeEach
    void setUp() {
        orcamento = new Orcamento();
        orcamento.setId(1L);
        orcamento.setNumero("2026-0001");
        orcamento.setCliente(new Cliente());
        orcamento.getCliente().setNome("Cliente Teste");
        orcamento.setStatusOrcamento(StatusOrcamento.SOLICITADO);
        orcamento.setDataEmissao(LocalDate.now());
        orcamento.setValidadeDias(15);
        when(orcamentoService.buscarPorId(anyLong())).thenReturn(orcamento);
    }

    private ItemOrcamento itemComOrdem(String nome, int ordem) {
        ItemOrcamento item = new ItemOrcamento();
        item.setNome(nome);
        item.setQuantidade(1);
        item.setPrecoUnitario(BigDecimal.TEN);
        item.setValorTotal(BigDecimal.TEN);
        item.setOrdem(ordem);
        return item;
    }

    @Test
    void itensAparecemNoHtmlNaOrdemFornecidaPelaLista() {
        ItemOrcamento segundoNaLista = itemComOrdem("Bancada de Granito", 1);
        ItemOrcamento primeiroNaLista = itemComOrdem("Soleira de Marmore", 0);
        orcamento.setItemOrcamentoList(List.of(primeiroNaLista, segundoNaLista));

        String html = service.gerarHtml(1L, null);

        int posicaoPrimeiro = html.indexOf("Soleira de Marmore");
        int posicaoSegundo = html.indexOf("Bancada de Granito");

        assertThat(posicaoPrimeiro).isPositive();
        assertThat(posicaoSegundo).isPositive();
        assertThat(posicaoPrimeiro).isLessThan(posicaoSegundo);
    }
}
```

Note the mocking shape: `orcamentoService` is a Mockito mock (not the real class), so its `calcularValorTotal(orcamento)` call inside `gerarHtml()` is a no-op by default — the `valorTotal`/`ordem` set directly on the fixture items above are exactly what ends up in the rendered HTML, nothing gets recalculated out from under the test.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./mvnw -q -Dtest=OrcamentoDocumentoServiceTest test
```

This test actually can't meaningfully fail on RED here, since it exercises pre-existing, already-correct behavior (the template already preserves list order — that was true before this plan too). Run it anyway to confirm it passes cleanly as a baseline:

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoServiceTest.java
git commit -m "test: cover item order preservation in generated document HTML"
```

---

## Self-Review

**Spec coverage:** Migration + backfill (spec section 3.1) → Task 1 Step 1. Entity field + `@OrderBy` (3.2) → Task 1 Steps 2–3. Assignment on save (3.3) → Task 2. Template needing no changes (3.4) → confirmed, not contradicted, by Task 3's test. Edge cases from the spec (pre-migration orçamento re-saved, item deleted mid-list, unused standalone `/api/item_orcamento` endpoints) don't need dedicated tasks — they're properties of the design (recompute-from-scratch-every-save), not separate code paths to build.

**Placeholder scan:** No TBD/TODO; every step shows complete code.

**Type consistency:** `ItemOrcamento.ordem` is `Integer` (Task 1); `OrcamentoService` sets it with `item.setOrdem(i)` where `i` is a primitive `int` (autoboxes cleanly, Task 2); the test in Task 2 asserts `getOrdem()` against `int` literals via AssertJ's `isEqualTo`, which handles the `Integer`/`int` comparison the same way the rest of this test file already does elsewhere.

**Verified against the real codebase before writing this plan:** every code change in Tasks 1–3 was implemented directly in the worktree, compiled (`./mvnw test-compile`, clean), and the two new tests (Task 2's and Task 3's) were run for real and passed, before being reverted to write this document from a known-working state rather than from derivation alone.
