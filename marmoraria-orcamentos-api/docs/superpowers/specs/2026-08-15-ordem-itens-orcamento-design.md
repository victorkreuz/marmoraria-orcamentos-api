# Design: Ordem dos itens no orçamento

**Data:** 2026-08-15
**Status:** Aprovado
**Repositório afetado:** `marmoraria-orcamentos-api` (backend only — sem mudança necessária no frontend)
**Item do backlog:** 3

---

## 1. Contexto / Problema

Pedido: os itens devem aparecer no documento gerado na ordem em que foram adicionados ao orçamento, não na ordem em que estão cadastrados na base de produtos/serviços.

### Causa raiz confirmada

Não existe nenhuma ordem persistida hoje. `ItemOrcamento` não tem `@OrderBy` nem coluna de posição — diferente da entidade irmã `ItemOrcamentoImagem`, que já tem exatamente esse padrão (campo `ordem` + `@OrderBy("ordem ASC, id ASC")` em `ItemOrcamento.imagens`) para as imagens de cada item.

Sem uma ordem explícita, o Postgres é livre para devolver as linhas de `item_orcamento` na ordem que achar mais conveniente numa consulta sem `ORDER BY`. Na prática, essa ordem tende a "andar" ao longo do tempo conforme o orçamento é editado (um `UPDATE` pode realocar a linha fisicamente na tabela) — não é garantidamente ordem de inserção nem ordem de cadastro no catálogo de produtos, é simplesmente indefinida.

## 2. Decisão

Persistir a posição de cada item, seguindo o mesmo padrão já usado em `ItemOrcamentoImagem`, e atribuí-la automaticamente no backend a partir da ordem em que os itens chegam no payload de salvar/editar — sem precisar de nenhuma mudança no frontend, já que ele só adiciona itens no fim do array local e nunca reordena (confirmado em `OrcamentoFormPage.tsx`: `addItem()` sempre faz `setItens(prev => [...prev, emptyItem()])`, e não existe nenhum controle de mover/reordenar item na UI).

## 3. Design

### 3.1 Migration `V3__add_ordem_item_orcamento.sql`

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

A coluna fica nullable — itens novos sempre recebem valor no momento do save (ver 3.3), e um eventual `NULL` remanescente ordena por último, já que o Postgres trata `NULL` como "maior" por padrão em `ORDER BY ... ASC` — mesmo comportamento tolerante que `ItemOrcamentoImagem` já assume hoje, sem precisar de `NOT NULL` nem de `NULLS LAST` explícito.

### 3.2 Entidade

`ItemOrcamento` ganha `private Integer ordem;`. `Orcamento.itemOrcamentoList` ganha `@OrderBy("ordem ASC, id ASC")` — mesma anotação, mesmo padrão já usado em `ItemOrcamento.imagens`.

### 3.3 Atribuição da ordem

`OrcamentoService.calcularValorTotal()` já percorre `orcamento.getItemOrcamentoList()` em todo save/edit (para somar o total do orçamento). Esse loop existente passa a gravar `item.setOrdem(indice)` para cada item, na posição em que aparece na lista recebida — sem endpoint novo, sem mudança no formato do payload.

### 3.4 Template de geração

`OrcamentoDocumentoService.paginaItens()` já apenas itera `orcamento.getItemOrcamentoList()` na ordem em que a coleção chega do banco. Uma vez que a query volta ordenada pelo `@OrderBy` da entidade, o item 3 do backlog fica resolvido automaticamente — nenhuma mudança necessária no template nem no service de geração além do que já está descrito acima.

## 4. Casos de borda

- **Orçamento existente (pré-migration) editado pela primeira vez após o deploy:** os itens já têm `ordem` populada pelo backfill, então a lista carregada na tela de edição já vem ordenada; ao salvar, `calcularValorTotal()` regrava `ordem` pela posição atual da lista — comportamento estável, não depende de nenhum estado transitório.
- **Exclusão de um item no meio da lista:** os valores de `ordem` dos itens restantes não precisam formar uma sequência contígua entre um save e outro, porque a cada save inteiro do orçamento a ordem é sempre recalculada do zero pela posição atual na lista recebida.
- **Endpoints avulsos de `/api/item_orcamento` (POST/PUT direto, fora do fluxo de `/api/orcamento`):** confirmado que o frontend nunca usa esse caminho hoje — não existe um `itemOrcamento.service.ts` no frontend, todo CRUD de item acontece através do objeto `Orcamento` aninhado via `/api/orcamento`. A atribuição de `ordem` descrita aqui fica só no caminho de `OrcamentoService`, que é o único exercitado pelo sistema real. Se esse endpoint avulso vier a ser usado no futuro, um item salvo por ele manteria a `ordem` que já tinha (ou `null`, se novo) — corrigir esse caminho hoje não utilizado está fora do escopo deste trabalho.

## 5. Testes

Backend, mesmo padrão de `ItemOrcamentoServiceTest`/`OrcamentoServiceTest` (JUnit + Mockito + AssertJ):

- Salvar orçamento com 3 itens na ordem [C, A, B] → `ordem` gravado como [0, 1, 2] respectivamente, na mesma sequência de chegada.
- Editar um orçamento existente removendo o item do meio e salvando → os itens restantes recebem nova `ordem` [0, 1] pela posição atual, sem depender dos valores antigos.
- `OrcamentoDocumentoService`: gerar HTML de um orçamento com itens cuja `ordem` é diferente da ordem de `id` → itens aparecem no HTML na sequência de `ordem`, não de `id`.

## 6. Fora de escopo

- UI de reordenar itens manualmente (drag-and-drop, mover para cima/baixo) — não foi pedido; a ordem é só "ordem de adição", que já é o comportamento natural da UI atual.
- Fazer os endpoints avulsos de `/api/item_orcamento` também atribuírem `ordem` — não são usados pelo frontend hoje.
