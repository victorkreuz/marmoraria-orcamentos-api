# Documento Gerado — Projeto, Imagens, Tipografia e Resumo Financeiro Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix four related problems in the generated commercial proposal: empty project fields/images that shouldn't render, cropped project photos, undersized item description text, and the total value leaking into "Condições de Pagamento" even when "ocultar total geral" is checked.

**Architecture:** Split the project section's single template and single visibility flag into two independently-gated fragments (text, images). Change the image gallery's CSS from crop-to-fill to show-the-whole-image. Bump the item description font size and tighten surrounding spacing to compensate. Replace the hardcoded financial-summary block inside "Condições de Pagamento" with a placeholder gated by the same flag that already hides the item table's total row.

**Tech Stack:** Spring Boot 3 / Java 17, plain HTML/CSS templates (no templating engine — string placeholder substitution), JUnit 5 + Mockito + AssertJ (backend); React 18 + TypeScript (small frontend piece)

**Repositories:** `marmoraria-orcamentos-api` (Tasks 1–4) and `marmoraria-orcamentos-web` (Task 5). Every backend option added here is purely additive (see the compatibility note below) — the two repos' branches can be merged in either order.

**Spec:** `docs/superpowers/specs/2026-08-15-documento-gerado-design.md`

**Compatibility note (carried over from the spec):** `imprimirProjeto`/`ocultarProjeto` keep their existing name and meaning; `imprimirProjetoImagens`/`ocultarProjetoImagens` are new fields added alongside them. Nothing existing is renamed, so a frontend and backend on either side of this change keep working correctly against each other.

**Suggested order:** implement `2026-08-15-ordem-itens-orcamento.md` before this plan. That plan creates `OrcamentoDocumentoServiceTest.java` with one test in it; Task 1 below adds more tests to that same file. If you're doing this plan first for some reason, Task 1 Step 1 includes the full file shell to create it fresh instead.

---

## File Structure

- Modify: `src/main/java/com/marmoraria/orcamentos/dto/OpcoesGeracaoRequest.java` — add `imprimirProjetoImagens`.
- Modify: `src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java` — split project rendering, gate the financial-summary block.
- Delete: `src/main/resources/templates/orcamento/projeto.html`
- Create: `src/main/resources/templates/orcamento/projeto-info.html`
- Create: `src/main/resources/templates/orcamento/projeto-galeria.html`
- Modify: `src/main/resources/templates/orcamento/identificacao.html`
- Modify: `src/main/resources/templates/orcamento/totais.html`
- Modify: `src/main/resources/templates/orcamento/orcamento.css` — `object-fit`, font size, spacing.
- Modify: `src/test/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoServiceTest.java`
- Modify (in `marmoraria-orcamentos-web`): `src/types/api.types.ts`, `src/pages/OrcamentoDetailPage/OrcamentoDetailPage.tsx`, `src/api/orcamentos.service.ts`

All backend paths below are relative to the `marmoraria-orcamentos-api` project root; Task 5's paths are relative to the `marmoraria-orcamentos-web` project root.

---

### Task 1: Split the project section into independent text/image toggles (TDD)

**Files:**
- Modify: `src/main/java/com/marmoraria/orcamentos/dto/OpcoesGeracaoRequest.java`
- Delete: `src/main/resources/templates/orcamento/projeto.html`
- Create: `src/main/resources/templates/orcamento/projeto-info.html`
- Create: `src/main/resources/templates/orcamento/projeto-galeria.html`
- Modify: `src/main/resources/templates/orcamento/identificacao.html`
- Modify: `src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java`
- Modify: `src/test/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoServiceTest.java`

- [ ] **Step 1: Write the failing tests**

If `src/test/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoServiceTest.java` doesn't exist yet, create it with this full shell:

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
}
```

(If the file already exists from the `ordem-itens-orcamento` plan, it already has this shell plus one test, `itensAparecemNoHtmlNaOrdemFornecidaPelaLista` — leave that test as-is.)

Add these three tests to the class, using the `import com.marmoraria.orcamentos.entity.Projeto;` import (add it alongside the other entity imports at the top):

```java
    @Test
    void projetoTotalmenteVazioNaoAparaceNoHtml() {
        orcamento.setProjeto(null);

        String html = service.gerarHtml(1L, null);

        assertThat(html).doesNotContain("Dados do Projeto");
    }

    @Test
    void projetoComApenasNomePreenchidoMostraSoEsseCampo() {
        Projeto projeto = new Projeto();
        projeto.setNome("Cozinha Gourmet");
        orcamento.setProjeto(projeto);

        String html = service.gerarHtml(1L, null);

        assertThat(html).contains("Dados do Projeto");
        assertThat(html).contains("Nome do Projeto");
        assertThat(html).contains("Cozinha Gourmet");
        assertThat(html).doesNotContain("Material / Ambiente");
        assertThat(html).doesNotContain("Responsável Técnico");
        assertThat(html).doesNotContain("Observações do Projeto");
    }

    @Test
    void projetoSemImagensNaoMostraSecaoDeGaleriaNemPlaceholder() {
        Projeto projeto = new Projeto();
        projeto.setNome("Cozinha Gourmet");
        orcamento.setProjeto(projeto);

        String html = service.gerarHtml(1L, null);

        assertThat(html).doesNotContain("Visualização do Projeto");
        // "image-placeholder" as a CSS class name always appears (the stylesheet is inlined into
        // every generated document), so check for the placeholder element's own text instead of
        // the class name — a naive doesNotContain("image-placeholder") assertion here is always
        // false regardless of whether the element itself renders.
        assertThat(html).doesNotContain("Imagem do projeto");
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./mvnw -q -Dtest=OrcamentoDocumentoServiceTest test
```

Expected: FAIL — `projetoComApenasNomePreenchidoMostraSoEsseCampo` and `projetoSemImagensNaoMostraSecaoDeGaleriaNemPlaceholder` fail because today's `paginaProjeto()` always renders every field (with a "-" fallback) and always renders a placeholder box when there's no image. `projetoTotalmenteVazioNaoAparaceNoHtml` fails for the same reason — the section always renders today, regardless of content.

- [ ] **Step 3: Add the new option flag**

In `src/main/java/com/marmoraria/orcamentos/dto/OpcoesGeracaoRequest.java`:

```java
package com.marmoraria.orcamentos.dto;

import lombok.Data;

@Data
public class OpcoesGeracaoRequest {
    private Boolean imprimirCapa;
    private Boolean imprimirTotal;
    private Boolean imprimirProjeto;
    private Boolean imprimirProjetoImagens;

    public boolean isImprimirCapaAtivo() {
        return Boolean.TRUE.equals(imprimirCapa);
    }

    public boolean isImprimirTotalAtivo() {
        return Boolean.TRUE.equals(imprimirTotal);
    }

    public boolean isImprimirProjetoAtivo() {
        return !Boolean.FALSE.equals(imprimirProjeto); // default true se não informado
    }

    public boolean isImprimirProjetoImagensAtivo() {
        return !Boolean.FALSE.equals(imprimirProjetoImagens); // default true se não informado
    }

    public static OpcoesGeracaoRequest padrao() {
        OpcoesGeracaoRequest opcoes = new OpcoesGeracaoRequest();
        opcoes.setImprimirCapa(false);
        opcoes.setImprimirTotal(true);
        opcoes.setImprimirProjeto(true);
        opcoes.setImprimirProjetoImagens(true);
        return opcoes;
    }
}
```

- [ ] **Step 4: Split the template**

Delete `src/main/resources/templates/orcamento/projeto.html`.

Create `src/main/resources/templates/orcamento/projeto-info.html`:

```html
<section id="project-summary" class="pdf-module module-project-summary module-with-divider no-break">
    <div class="section-header">
        <h2 class="section-title">Dados do Projeto</h2>
    </div>

    <div class="client-grid client-grid--3col">
        [[CAMPOS_PROJETO_GRID]]
    </div>

    [[CAMPOS_PROJETO_OBSERVACOES]]
</section>
```

Create `src/main/resources/templates/orcamento/projeto-galeria.html`:

```html
<section id="gallery-images" class="pdf-module module-gallery module-with-divider">
    <div class="section-header">
        <h2 class="section-title">Visualização do Projeto</h2>
    </div>

    <div class="gallery-grid">
        [[IMAGENS_PROJETO]]
    </div>
</section>
```

In `src/main/resources/templates/orcamento/identificacao.html`:

```html
    [[SECAO_PROJETO]]

</section>
```

becomes:

```html
    [[SECAO_PROJETO_TEXTO]]
    [[SECAO_PROJETO_IMAGENS]]

</section>
```

- [ ] **Step 5: Rewrite the service methods**

In `src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java`, inside `paginaIdentificacao(...)`:

```java
        dados.put("DATA_VENCIMENTO", esc(formatarData(vencimento)));
        dados.put("SECAO_PROJETO", opcoes.isImprimirProjetoAtivo() ? paginaProjeto(orcamento, responsavelTecnico) : "");

        return renderizar("identificacao.html", dados);
    }
```

becomes:

```java
        dados.put("DATA_VENCIMENTO", esc(formatarData(vencimento)));
        dados.put("SECAO_PROJETO_TEXTO", opcoes.isImprimirProjetoAtivo() ? paginaProjetoTexto(orcamento, responsavelTecnico) : "");
        dados.put("SECAO_PROJETO_IMAGENS", opcoes.isImprimirProjetoImagensAtivo() ? paginaProjetoImagens(orcamento) : "");

        return renderizar("identificacao.html", dados);
    }
```

Then replace `paginaProjeto(...)` and `imagensProjetoHtml(...)` entirely:

```java
    private String paginaProjeto(Orcamento orcamento, String responsavelTecnico) {
        Projeto projeto = orcamento.getProjeto();
        String nome = projeto == null ? "Projeto" : valor(projeto.getNome());
        String tipoPedra = projeto == null ? "Tipo de pedra a definir" : valor(projeto.getTipoPedraPrincipal());

        Map<String, String> dados = contextoBase();
        dados.put("NOME_PROJETO", esc(nome));
        dados.put("AMBIENTE_PROJETO", esc(tipoPedra));
        dados.put("TIPO_PEDRA_PROJETO", esc(tipoPedra));
        dados.put("OBSERVACOES_PROJETO", esc(valor(projeto == null ? null : projeto.getObservacoes())));
        dados.put("IMAGENS_PROJETO", imagensProjetoHtml(projeto));
        dados.put("RESPONSAVEL_TECNICO", esc(responsavelTecnico));

        return renderizar("projeto.html", dados);
    }

    private String imagensProjetoHtml(Projeto projeto) {
        List<ProjetoImagem> imagens = projeto == null || projeto.getImagens() == null ? List.of() : projeto.getImagens();
        if (!imagens.isEmpty()) {
            StringBuilder html = new StringBuilder();
            for (ProjetoImagem imagem : imagens) {
                if (isBlank(imagem.getUrl())) {
                    continue;
                }
                html.append("<figure class=\"gallery-figure\">");
                if (tituloValido(imagem.getTitulo())) {
                    html.append("<figcaption>").append(esc(imagem.getTitulo())).append("</figcaption>");
                }
                html.append("<img class=\"gallery-grid__item\" src=\"")
                        .append(esc(imagem.getUrl()))
                        .append("\" alt=\"")
                        .append(esc(valor(imagem.getTitulo())))
                        .append("\" />");
                html.append("</figure>");
            }
            if (!html.isEmpty()) {
                return html.toString();
            }
        }

        String foto = projeto == null ? null : projeto.getFotoPrincipalUrl();
        return imagemOuPlaceholder(foto, "gallery-grid__item gallery-grid__item--wide", "Foto do projeto", "Imagem do projeto");
    }
```

with:

```java
    private String paginaProjetoTexto(Orcamento orcamento, String responsavelTecnico) {
        Projeto projeto = orcamento.getProjeto();
        String nome = projeto == null ? null : projeto.getNome();
        String tipoPedra = projeto == null ? null : projeto.getTipoPedraPrincipal();
        String observacoes = projeto == null ? null : projeto.getObservacoes();

        if (isBlank(nome) && isBlank(tipoPedra) && isBlank(responsavelTecnico) && isBlank(observacoes)) {
            return "";
        }

        StringBuilder grid = new StringBuilder();
        infoBlockSe(grid, "Nome do Projeto", nome);
        infoBlockSe(grid, "Material / Ambiente", tipoPedra);
        infoBlockSe(grid, "Responsável Técnico", responsavelTecnico);

        Map<String, String> dados = contextoBase();
        dados.put("CAMPOS_PROJETO_GRID", grid.toString());
        dados.put("CAMPOS_PROJETO_OBSERVACOES", isBlank(observacoes) ? "" :
            "<div class=\"info-block info-block--full\">" +
            "<span class=\"info-block__label\">Observações do Projeto</span>" +
            "<span class=\"info-block__value text-preserve-lines\">" + esc(observacoes) + "</span>" +
            "</div>");

        return renderizar("projeto-info.html", dados);
    }

    private String paginaProjetoImagens(Orcamento orcamento) {
        String imagensHtml = imagensProjetoHtml(orcamento.getProjeto());
        if (isBlank(imagensHtml)) {
            return "";
        }
        Map<String, String> dados = contextoBase();
        dados.put("IMAGENS_PROJETO", imagensHtml);
        return renderizar("projeto-galeria.html", dados);
    }

    private String imagensProjetoHtml(Projeto projeto) {
        List<ProjetoImagem> imagens = projeto == null || projeto.getImagens() == null ? List.of() : projeto.getImagens();
        if (!imagens.isEmpty()) {
            StringBuilder html = new StringBuilder();
            for (ProjetoImagem imagem : imagens) {
                if (isBlank(imagem.getUrl())) {
                    continue;
                }
                html.append("<figure class=\"gallery-figure\">");
                if (tituloValido(imagem.getTitulo())) {
                    html.append("<figcaption>").append(esc(imagem.getTitulo())).append("</figcaption>");
                }
                html.append("<img class=\"gallery-grid__item\" src=\"")
                        .append(esc(imagem.getUrl()))
                        .append("\" alt=\"")
                        .append(esc(valor(imagem.getTitulo())))
                        .append("\" />");
                html.append("</figure>");
            }
            if (!html.isEmpty()) {
                return html.toString();
            }
        }

        String foto = projeto == null ? null : projeto.getFotoPrincipalUrl();
        if (isBlank(foto)) {
            return "";
        }
        return "<img class=\"gallery-grid__item gallery-grid__item--wide\" src=\"" + esc(foto) + "\" alt=\"Foto do projeto\" />";
    }
```

The key differences: `paginaProjetoTexto` now takes an early exit returning `""` when every field is blank, and builds the grid with `infoBlockSe` (which already exists in this file, used today for the client's extra fields) instead of always filling in a value or a "-" fallback. `imagensProjetoHtml`'s last line no longer falls back to `imagemOuPlaceholder(...)` — it returns `""` when there's no real photo, which `paginaProjetoImagens` uses to decide whether to render the gallery section at all. `imagemOuPlaceholder(...)` itself is untouched — it's still used for item thumbnails in `paginaItens()`, unaffected by this change.

- [ ] **Step 6: Run the tests to verify they pass**

```bash
./mvnw -q -Dtest=OrcamentoDocumentoServiceTest test
```

Expected: PASS — all tests in the file green (4 if the file was created fresh in this task, one more if `ordem-itens-orcamento` already added its own).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/marmoraria/orcamentos/dto/OpcoesGeracaoRequest.java src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java src/main/resources/templates/orcamento/projeto.html src/main/resources/templates/orcamento/projeto-info.html src/main/resources/templates/orcamento/projeto-galeria.html src/main/resources/templates/orcamento/identificacao.html src/test/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoServiceTest.java
git commit -m "feat: split project section into independent text/image toggles, drop empty fields and placeholder"
```

---

### Task 2: Stop cropping project photos

**Files:**
- Modify: `src/main/resources/templates/orcamento/orcamento.css`

No test — this is a pure CSS value change with no observable string-level effect the existing test suite can check (both `object-fit: cover` and `object-fit: contain` produce the exact same HTML, just different rendering). Verified visually in Task 6.

- [ ] **Step 1: Change the fit mode**

In `src/main/resources/templates/orcamento/orcamento.css`:

```css
.gallery-grid__item {
    aspect-ratio: 4 / 3;
    border-radius: var(--border-radius);
    display: block;
    object-fit: cover;
    width: 100%;
}
```

becomes:

```css
.gallery-grid__item {
    aspect-ratio: 4 / 3;
    border-radius: var(--border-radius);
    display: block;
    object-fit: contain;
    width: 100%;
}
```

`.gallery-grid__item--wide` (the single-photo fallback layout) inherits this through the shared `.gallery-grid__item` class, so no separate change is needed there. `.product-thumb` (the small 34×34px item thumbnail in the products table) is intentionally left alone — see "Out of scope" in the spec.

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/orcamento/orcamento.css
git commit -m "fix: stop cropping project photos in generated document (object-fit: contain)"
```

---

### Task 3: Increase item description font size

**Files:**
- Modify: `src/main/resources/templates/orcamento/orcamento.css`

No test, same reasoning as Task 2 — verified visually in Task 6.

- [ ] **Step 1: Bump the font size and tighten surrounding spacing**

In `src/main/resources/templates/orcamento/orcamento.css`:

```css
.products-table tbody td {
    color: #1f2430;
    font-size: 9px;
    line-height: 1.35;
    padding: 8px;
    vertical-align: middle;
}
```

becomes:

```css
.products-table tbody td {
    color: #1f2430;
    font-size: 9px;
    line-height: 1.35;
    padding: 6px 8px;
    vertical-align: middle;
}
```

and:

```css
.products-table .product-name {
    color: #1f2430;
    font-size: 9px;
    font-weight: var(--fw-bold);
    margin-bottom: 4px;
}

.products-table .product-desc {
    color: #4a4f5c;
    font-size: 7.5px;
    font-weight: var(--fw-regular);
    line-height: 1.35;
}
```

becomes:

```css
.products-table .product-name {
    color: #1f2430;
    font-size: 9px;
    font-weight: var(--fw-bold);
    margin-bottom: 2px;
}

.products-table .product-desc {
    color: #4a4f5c;
    font-size: 9.5px;
    font-weight: var(--fw-regular);
    line-height: 1.3;
}
```

These are starting values — the point of the change (7.5px is too small) is fixed either way; the exact final numbers may need a small further nudge once you look at a real generated PDF in Task 6. If a row still looks too tall with several lines of description, tighten `padding` a bit more before moving on; don't spend time perfecting this from CSS alone without looking at the rendered output.

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/orcamento/orcamento.css
git commit -m "fix: increase item description font size, tighten row spacing to compensate"
```

---

### Task 4: Stop the total from leaking into "Condições de Pagamento" (TDD)

**Files:**
- Modify: `src/main/resources/templates/orcamento/totais.html`
- Modify: `src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java`
- Modify: `src/test/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Add to `OrcamentoDocumentoServiceTest.java` (the `import java.util.List;` needed below is already present in the file from the `ordem-itens-orcamento` plan's test — if this is the first test in the file to use `Financeiro`, also add `import com.marmoraria.orcamentos.entity.Financeiro;` and `import com.marmoraria.orcamentos.dto.GerarOrcamentoRequest;` and `import com.marmoraria.orcamentos.dto.OpcoesGeracaoRequest;` alongside the other imports):

```java
    private Financeiro financeiroComTotal(String totalFinal) {
        Financeiro financeiro = new Financeiro();
        financeiro.setSubtotalItens(new BigDecimal("9999.00"));
        financeiro.setTotalFinal(new BigDecimal(totalFinal));
        return financeiro;
    }

    @Test
    void ocultarTotalGeralRemoveOValorDasDuasOcorrencias() {
        orcamento.setFinanceiro(financeiroComTotal("1234.56"));
        orcamento.setItemOrcamentoList(List.of(itemComOrdem("Item Unico", 0)));
        GerarOrcamentoRequest request = new GerarOrcamentoRequest();
        OpcoesGeracaoRequest opcoes = new OpcoesGeracaoRequest();
        opcoes.setImprimirTotal(false);
        request.setOpcoes(opcoes);

        String html = service.gerarHtml(1L, request);

        assertThat(html).doesNotContain("1.234,56");
    }

    @Test
    void semOcultarTotalGeralOValorApareceNasDuasOcorrencias() {
        orcamento.setFinanceiro(financeiroComTotal("1234.56"));
        orcamento.setItemOrcamentoList(List.of(itemComOrdem("Item Unico", 0)));

        String html = service.gerarHtml(1L, null);

        long ocorrencias = html.split("1\\.234,56", -1).length - 1;
        assertThat(ocorrencias).isGreaterThanOrEqualTo(2);
    }
```

The fixture deliberately uses a `subtotalItens` (9999.00) different from `totalFinal` (1234.56) so the two assertions are unambiguous about which figure they're tracking — `resumoFinanceiroItem` (Step 3 below) always shows subtotal, so if the test used equal values it couldn't distinguish "total correctly hidden" from "total happened to equal the still-visible subtotal."

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./mvnw -q -Dtest=OrcamentoDocumentoServiceTest#ocultarTotalGeralRemoveOValorDasDuasOcorrencias,OrcamentoDocumentoServiceTest#semOcultarTotalGeralOValorApareceNasDuasOcorrencias test
```

Expected: `ocultarTotalGeralRemoveOValorDasDuasOcorrencias` FAILS — today, "1.234,56" still appears once (inside the "Resumo financeiro" block in Condições de Pagamento, via the always-added "Total final" line... actually via "Subtotal dos itens" not matching in this fixture, but "Total final" is currently skipped by the flag in that same block too — check the actual failure message to confirm, but the point of this test is exactly the bug this task fixes). `semOcultarTotalGeralOValorApareceNasDuasOcorrencias` should already PASS today (it's a baseline sanity check, not testing new behavior).

- [ ] **Step 3: Gate the whole block, not just the total line**

In `src/main/resources/templates/orcamento/totais.html`:

```html
            <li class="payment-item">
                <span class="payment-item__label">Prazo de produção</span>
                <span class="payment-item__value">[[PRAZO_PRODUCAO]]</span>
            </li>
            <li class="payment-item payment-item--full">
                <span class="payment-item__label">Resumo financeiro</span>
                <span class="payment-item__value">
                    <table class="totals-table">
                        [[LINHAS_TOTAIS]]
                    </table>
                </span>
            </li>
        </ul>
```

becomes:

```html
            <li class="payment-item">
                <span class="payment-item__label">Prazo de produção</span>
                <span class="payment-item__value">[[PRAZO_PRODUCAO]]</span>
            </li>
            [[RESUMO_FINANCEIRO_ITEM]]
        </ul>
```

In `src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java`, replace `paginaTotais(...)`:

```java
    private String paginaTotais(Orcamento orcamento, OpcoesGeracaoRequest opcoes, String responsavelTecnico, String observacoesDocumento) {
        Financeiro financeiro = financeiroOuVazio(orcamento);
        StringBuilder linhas = new StringBuilder();
        linhas.append(linhaTotal("Subtotal dos itens", formatarMoeda(financeiro.getSubtotalItens()), false));

        if (Boolean.FALSE.equals(financeiro.getFreteIncluso())
                && valorOuZero(financeiro.getFreteExtra()).compareTo(BigDecimal.ZERO) > 0) {
            linhas.append(linhaTotal("Frete", formatarMoeda(financeiro.getFreteExtra()), false));
        }

        if (valorOuZero(financeiro.getDescontoPercentual()).compareTo(BigDecimal.ZERO) > 0) {
            linhas.append(linhaTotal("Desconto (" + financeiro.getDescontoPercentual() + "%)", "- " + formatarMoeda(financeiro.getDescontoValorReais()), false));
        } else if (valorOuZero(financeiro.getDescontoValorReais()).compareTo(BigDecimal.ZERO) > 0) {
            linhas.append(linhaTotal("Desconto", "- " + formatarMoeda(financeiro.getDescontoValorReais()), false));
        }
        if (valorOuZero(financeiro.getAdendos()).compareTo(BigDecimal.ZERO) > 0) {
            linhas.append(linhaTotal("Adendos / Acréscimos", formatarMoeda(financeiro.getAdendos()), false));
        }
        if (opcoes.isImprimirTotalAtivo()) {
            linhas.append(linhaTotal("Total final", formatarMoeda(financeiro.getTotalFinal()), true));
        }

        Map<String, String> dados = contextoBase();
        dados.put("SECTION_CLASS", "doc-section-inline");
        dados.put("LINHAS_TOTAIS", linhas.toString());
        dados.put("MEIO_PAGAMENTO_ITEM", meioPagamentoItem(financeiro));
        dados.put("PRAZO_PRODUCAO", esc(prazoProducao(orcamento)));
        dados.put("NOME_CLIENTE", esc(nomeCliente(orcamento)));
        dados.put("RESPONSAVEL_TECNICO", esc(responsavelTecnico));
        String obsItens = obsDocumentoItens(observacoesDocumento);
        dados.put("SECAO_OBS_DOCUMENTO", isBlank(obsItens) ? "" :
            "<section class=\"pdf-module module-with-divider no-break\">" +
            "<div class=\"section-header\"><h2 class=\"section-title\">Observações Importantes</h2></div>" +
            "<ul class=\"commercial-obs-list\">" + obsItens + "</ul>" +
            "</section>");

        return renderizar("totais.html", dados);
    }
```

with:

```java
    private String paginaTotais(Orcamento orcamento, OpcoesGeracaoRequest opcoes, String responsavelTecnico, String observacoesDocumento) {
        Financeiro financeiro = financeiroOuVazio(orcamento);

        Map<String, String> dados = contextoBase();
        dados.put("SECTION_CLASS", "doc-section-inline");
        dados.put("MEIO_PAGAMENTO_ITEM", meioPagamentoItem(financeiro));
        dados.put("PRAZO_PRODUCAO", esc(prazoProducao(orcamento)));
        dados.put("RESUMO_FINANCEIRO_ITEM", opcoes.isImprimirTotalAtivo() ? resumoFinanceiroItem(financeiro) : "");
        dados.put("NOME_CLIENTE", esc(nomeCliente(orcamento)));
        dados.put("RESPONSAVEL_TECNICO", esc(responsavelTecnico));
        String obsItens = obsDocumentoItens(observacoesDocumento);
        dados.put("SECAO_OBS_DOCUMENTO", isBlank(obsItens) ? "" :
            "<section class=\"pdf-module module-with-divider no-break\">" +
            "<div class=\"section-header\"><h2 class=\"section-title\">Observações Importantes</h2></div>" +
            "<ul class=\"commercial-obs-list\">" + obsItens + "</ul>" +
            "</section>");

        return renderizar("totais.html", dados);
    }

    private String resumoFinanceiroItem(Financeiro financeiro) {
        StringBuilder linhas = new StringBuilder();
        linhas.append(linhaTotal("Subtotal dos itens", formatarMoeda(financeiro.getSubtotalItens()), false));

        if (Boolean.FALSE.equals(financeiro.getFreteIncluso())
                && valorOuZero(financeiro.getFreteExtra()).compareTo(BigDecimal.ZERO) > 0) {
            linhas.append(linhaTotal("Frete", formatarMoeda(financeiro.getFreteExtra()), false));
        }

        if (valorOuZero(financeiro.getDescontoPercentual()).compareTo(BigDecimal.ZERO) > 0) {
            linhas.append(linhaTotal("Desconto (" + financeiro.getDescontoPercentual() + "%)", "- " + formatarMoeda(financeiro.getDescontoValorReais()), false));
        } else if (valorOuZero(financeiro.getDescontoValorReais()).compareTo(BigDecimal.ZERO) > 0) {
            linhas.append(linhaTotal("Desconto", "- " + formatarMoeda(financeiro.getDescontoValorReais()), false));
        }
        if (valorOuZero(financeiro.getAdendos()).compareTo(BigDecimal.ZERO) > 0) {
            linhas.append(linhaTotal("Adendos / Acréscimos", formatarMoeda(financeiro.getAdendos()), false));
        }
        linhas.append(linhaTotal("Total final", formatarMoeda(financeiro.getTotalFinal()), true));

        return "<li class=\"payment-item payment-item--full\">" +
                "<span class=\"payment-item__label\">Resumo financeiro</span>" +
                "<span class=\"payment-item__value\">" +
                "<table class=\"totals-table\">" + linhas + "</table>" +
                "</span>" +
                "</li>";
    }
```

`resumoFinanceiroItem` is only ever called from behind `opcoes.isImprimirTotalAtivo()`, so "Total final" no longer needs its own inner `if` — the whole method (subtotal, frete, desconto, adendos, and total together) only runs when the flag is on. This also removes the `LINHAS_TOTAIS` key entirely — nothing else in the codebase reads it (the item table's own total footer, `TFOOT_TOTAIS`, is a separate, already-correctly-gated piece built by `tfootTotais(financeiro)`, untouched by this task).

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./mvnw -q -Dtest=OrcamentoDocumentoServiceTest test
```

Expected: PASS — every test in the file green.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/orcamento/totais.html src/main/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoService.java src/test/java/com/marmoraria/orcamentos/service/OrcamentoDocumentoServiceTest.java
git commit -m "fix: hide entire financial summary block in Condições de Pagamento when total is hidden"
```

---

### Task 5: Frontend — second checkbox for project images

**Files:**
- Modify: `src/types/api.types.ts`
- Modify: `src/pages/OrcamentoDetailPage/OrcamentoDetailPage.tsx`
- Modify: `src/api/orcamentos.service.ts`

No automated test coverage (see the auth plan for why) — verified manually together with Task 6.

- [ ] **Step 1: Add the option to the shared type**

In `src/types/api.types.ts`:

```ts
export interface GerarOrcamentoOptions {
  comCapa: boolean;
  ocultarTotalGeral: boolean;
  ocultarProjeto: boolean;
  observacoesDocumento: string;
}
```

becomes:

```ts
export interface GerarOrcamentoOptions {
  comCapa: boolean;
  ocultarTotalGeral: boolean;
  ocultarProjeto: boolean;
  ocultarProjetoImagens: boolean;
  observacoesDocumento: string;
}
```

- [ ] **Step 2: Map it to the backend payload**

In `src/api/orcamentos.service.ts`:

```ts
function toBackendPayload(options: GerarOrcamentoOptions) {
  return {
    opcoes: {
      imprimirCapa: options.comCapa,
      imprimirTotal: !options.ocultarTotalGeral,
      imprimirProjeto: !options.ocultarProjeto,
    },
    observacoesDocumento: options.observacoesDocumento || '',
  };
}
```

becomes:

```ts
function toBackendPayload(options: GerarOrcamentoOptions) {
  return {
    opcoes: {
      imprimirCapa: options.comCapa,
      imprimirTotal: !options.ocultarTotalGeral,
      imprimirProjeto: !options.ocultarProjeto,
      imprimirProjetoImagens: !options.ocultarProjetoImagens,
    },
    observacoesDocumento: options.observacoesDocumento || '',
  };
}
```

- [ ] **Step 3: Add the checkbox and its state**

In `src/pages/OrcamentoDetailPage/OrcamentoDetailPage.tsx`, the state declarations:

```tsx
  const [comCapa, setComCapa] = useState(false);
  const [ocultarTotalGeral, setOcultarTotalGeral] = useState(false);
  const [ocultarProjeto, setOcultarProjeto] = useState(false);
```

become:

```tsx
  const [comCapa, setComCapa] = useState(false);
  const [ocultarTotalGeral, setOcultarTotalGeral] = useState(false);
  const [ocultarProjeto, setOcultarProjeto] = useState(false);
  const [ocultarProjetoImagens, setOcultarProjetoImagens] = useState(false);
```

The `opcoes()` function:

```tsx
  function opcoes() {
    return { comCapa, ocultarTotalGeral, ocultarProjeto, observacoesDocumento };
  }
```

becomes:

```tsx
  function opcoes() {
    return { comCapa, ocultarTotalGeral, ocultarProjeto, ocultarProjetoImagens, observacoesDocumento };
  }
```

And the checkbox list:

```tsx
            <div className={styles.optionsList}>
              <CheckboxOption label="Ocultar total geral" checked={ocultarTotalGeral} onChange={setOcultarTotalGeral} />
              <CheckboxOption label="Ocultar informações do projeto" checked={ocultarProjeto} onChange={setOcultarProjeto} />
            </div>
```

becomes:

```tsx
            <div className={styles.optionsList}>
              <CheckboxOption label="Ocultar total geral" checked={ocultarTotalGeral} onChange={setOcultarTotalGeral} />
              <CheckboxOption label="Ocultar texto do projeto" checked={ocultarProjeto} onChange={setOcultarProjeto} />
              <CheckboxOption label="Ocultar imagens do projeto" checked={ocultarProjetoImagens} onChange={setOcultarProjetoImagens} />
            </div>
```

("Ocultar informações do projeto" is relabeled "Ocultar texto do projeto" now that there are two separate checkboxes — otherwise the two would read ambiguously side by side.)

- [ ] **Step 4: Commit**

```bash
git add src/types/api.types.ts src/api/orcamentos.service.ts src/pages/OrcamentoDetailPage/OrcamentoDetailPage.tsx
git commit -m "feat: add separate checkbox to hide project images in generated document"
```

---

### Task 6: Visual verification

This covers Tasks 2, 3, and 5 together — the CSS changes and the new checkbox are all things you need to actually look at, not just something a string assertion can confirm.

- [ ] **Step 1: Generate a real document**

With the backend running locally (`./mvnw spring-boot:run -Dspring-boot.run.profiles=local`) and the frontend pointed at it, open an existing orçamento that has project photos and at least one item with a longer description, then:

1. Click "Pré-visualizar" with both new checkboxes unchecked. **Expected:** project photos show in full (no cropped edges/measurements cut off, even if that means a blank strip on one side of a photo whose proportions don't match the box); item descriptions are legibly sized, not the previous “tiny” 7.5px; rows aren't dramatically taller than before.
2. Check "Ocultar imagens do projeto" only, re-preview. **Expected:** project text fields still show, "Visualização do Projeto" section is gone.
3. Uncheck that, check "Ocultar texto do projeto" only, re-preview. **Expected:** the reverse — photos still show, "Dados do Projeto" section is gone.
4. Check "Ocultar total geral", re-preview. **Expected:** no monetary total appears anywhere in the document — not in the item table footer, not in "Condições de Pagamento".
5. Open (or create) an orçamento whose project fields are all empty and that has no project photos. **Expected:** neither "Dados do Projeto" nor "Visualização do Projeto" appear at all — no empty section headers, no gray placeholder box.

- [ ] **Step 2: Note anything that needs a follow-up tweak**

If row height, exact font size, or spacing still looks off after Step 1, adjust the values from Tasks 2–3 directly (they're starting points, not final) and repeat Step 1 until it looks right. This isn't a separate task — fold any such tweak into an amended commit or a small follow-up commit before moving on.

---

## Self-Review

**Spec coverage:** Item 4 (project fields/images) → Task 1. Item 5 (cropped images) → Task 2. Item 6 (typography/spacing) → Task 3. Item 7 (total leak) → Task 4. The frontend checkbox split → Task 5. Visual sign-off → Task 6. The spec's "fora de escopo" items (`.product-thumb`, reactivating `paginaResumo()`/`resumo.html`, the cover/avisos/observações pages) have no corresponding task, intentionally.

**Placeholder scan:** No TBD/TODO; every step shows complete before/after code, including the full test-file shell for the case where this plan runs before `ordem-itens-orcamento`.

**Type consistency:** `isImprimirProjetoImagensAtivo()` (Task 1) mirrors the existing `isImprimirProjetoAtivo()` exactly — same null-handling convention. `RESUMO_FINANCEIRO_ITEM` (Task 4) is the one new `dados` map key introduced for that fix, used consistently in both the Java method and the template. `GerarOrcamentoOptions.ocultarProjetoImagens` (Task 5, frontend) maps to `imprimirProjetoImagens` (Task 1, backend) the same way the existing `ocultarProjeto` → `imprimirProjeto` pair already does.

**Verified against the real codebase before writing this plan:** every backend change in Tasks 1 and 4 (the `OpcoesGeracaoRequest` field, the template split, `paginaProjetoTexto`/`paginaProjetoImagens`, `resumoFinanceiroItem`, and all 6 new test methods) was implemented directly in the worktree and run for real — `./mvnw -Dtest=OrcamentoDocumentoServiceTest test` passed 6/6 — before being reverted to write this document. The first version of the "no placeholder box" test failed against a naive `doesNotContain("image-placeholder")` assertion, because that string is always present in the inlined stylesheet regardless of whether the placeholder element itself renders; the corrected assertion (checking for the placeholder's visible text instead) is what's written into Step 1 above.
