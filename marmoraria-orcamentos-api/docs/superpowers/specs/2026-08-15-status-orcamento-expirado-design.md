# Design: Status do orçamento — expiração automática por data

**Data:** 2026-08-15
**Status:** Aprovado
**Repositórios afetados:** `marmoraria-orcamentos-api` (cálculo) + `marmoraria-orcamentos-web` (consumo em 6 pontos de leitura)
**Item do backlog:** 2

---

## 1. Contexto / Problema

Pedido original: marcar automaticamente o orçamento como expirado quando `dataValidade` vencer. Escopo: só o status visual muda — o orçamento continua editável e a proposta continua podendo ser gerada normalmente mesmo expirado.

Três achados de investigação mudaram a implementação em relação ao pedido literal:

### 1.1 Enum: EXPIRADO, não VENCIDO

`StatusOrcamento` tem os valores `VENCIDO` e `EXPIRADO`. `VENCIDO` não é usado em nenhum lugar do sistema além da própria declaração do enum — sem label, sem UI, sem lógica de negócio. `EXPIRADO` é o que já está ligado ponta a ponta: label "Expirado" em `formatters.ts`, presente no union type `OrcamentoStatus` (frontend), `<option>` manual no dropdown de edição, e já contabilizado no bucket "Outros" do dashboard.

**Decisão:** usar `EXPIRADO`. `VENCIDO` permanece no enum sem uso — removê-lo não faz parte deste trabalho (baixo risco de deixar como está, já que não é uma constraint de banco, só um valor de enum nunca referenciado).

### 1.2 Campo de data: `dataEmissao + validadeDias`, não `dataValidade`

O campo `Orcamento.dataValidade` existe na entidade, mas o frontend nunca o preenche nem envia — só manda `validadeDias` (uma duração: 15 ou 30 dias). Na prática, `dataValidade` está sempre nulo no banco. Esse campo também carrega uma validação `@FutureOrPresent`, que rejeitaria salvar/editar qualquer orçamento já vencido caso o campo passasse a ser populado — uma armadilha para o requisito de "orçamento continua editável mesmo expirado".

Já existe um cálculo equivalente, hoje privado e usado só no cabeçalho do PDF: `OrcamentoDocumentoService.dataVencimento()`, que computa `dataEmissao.plusDays(validadeDias)`.

**Decisão:** a data de vencimento efetiva é `dataEmissao + validadeDias`. O campo `dataValidade` não é lido nem escrito por este trabalho.

### 1.3 Status elegíveis para expirar

Só `SOLICITADO` e `ENVIADO` (pendentes de resposta do cliente) podem virar `EXPIRADO` quando a data vence. `APROVADO`, `REJEITADO` e `RASCUNHO` nunca são sobrescritos pela regra de data — uma vez que a proposta foi aceita ou recusada, ou enquanto ainda é rascunho não enviado ao cliente, o vencimento deixa de ser uma informação relevante para o status.

### 1.4 Persistência: calculado, não gravado

O status calculado **não é gravado no banco**. `statusOrcamento` continua sendo exatamente o que foi definido manualmente (cadastro ou formulário de edição). O cálculo acontece a cada leitura/serialização, sem job agendado.

### 1.5 Alcance da regra

Confirmado com o usuário durante o brainstorming: o campo calculado precisa valer em **todo** lugar do sistema que conta, filtra ou exibe status para fins de análise — não só no badge de detalhe. Regra de negócio explícita: um orçamento Solicitado/Enviado que vence deve aparecer nas análises como expirado (segue contando, só que sob esse status), não continuar contando como Solicitado/Enviado.

**Auditoria feita (backend + frontend, dois repositórios):** ver seção 3.3.

## 2. Decisão

Adicionar um campo calculado e não persistido, `statusExibicao`, exposto pela API em toda leitura de `Orcamento`, e migrar todos os pontos de agregação/exibição do frontend para consumi-lo em vez do `statusOrcamento` bruto — exceto o formulário de edição, que continua lendo/gravando o valor bruto por definição.

## 3. Design

### 3.1 Backend — cálculo compartilhado de vencimento

Extrair `dataVencimento(Orcamento)` de dentro de `OrcamentoDocumentoService` (hoje privado, duplicaria a lógica) para um local compartilhado — método estático simples, sem dependências externas. `OrcamentoDocumentoService` passa a chamar esse método compartilhado em vez de manter sua cópia própria (pequena limpeza, sem mudança de comportamento no PDF).

### 3.2 Backend — `Orcamento.getStatusExibicao()`

```java
public StatusOrcamento getStatusExibicao() {
    boolean elegivel = statusOrcamento == StatusOrcamento.SOLICITADO
                     || statusOrcamento == StatusOrcamento.ENVIADO;
    LocalDate vencimento = OrcamentoDatas.vencimento(this);
    if (elegivel && vencimento != null && vencimento.isBefore(LocalDate.now())) {
        return StatusOrcamento.EXPIRADO;
    }
    return statusOrcamento;
}
```

A entidade usa field-access (`@Id` está em um campo), então um getter sem campo correspondente é invisível para o JPA/Hibernate automaticamente — não precisa de `@Transient` para não virar coluna, mas vale anotar mesmo assim por clareza para quem ler o código depois.

Como os controllers devolvem a entidade `Orcamento` diretamente como corpo de resposta (sem DTO), o Jackson serializa esse getter automaticamente como `"statusExibicao"` no JSON — não é necessário alterar `OrcamentoController`.

### 3.3 Frontend — auditoria completa e pontos de mudança

`api.types.ts`: adicionar `statusExibicao: OrcamentoStatus` à interface `Orcamento`, com comentário em cada um dos dois campos deixando o uso pretendido explícito (guarda contra regressão futura, já que o TypeScript não consegue forçar isso sozinho):

```ts
/** Valor bruto gravado no banco — usar apenas no formulário de edição. */
statusOrcamento: OrcamentoStatus;
/** Status calculado (Solicitado/Enviado vencidos viram Expirado) — usar em badges, contadores e filtros. */
statusExibicao: OrcamentoStatus;
```

Confirmado por busca em todo o backend (`OrcamentoRepository` não tem métodos próprios — só `extends JpaRepository`, sem `@Query`/`Specification`) que **não existe nenhum filtro ou contagem por status em SQL** hoje; tudo é `findAll()` seguido de agregação em memória no frontend. Por isso a auditoria abaixo cobre só o frontend — não há ponto nenhum no backend que precise "enxergar" o status calculado para filtrar/contar.

| Arquivo | Uso hoje | Muda comportamento? |
|---|---|---|
| `DashboardPage.tsx` — KPI "Pendentes" | conta Solicitado+Enviado brutos | **Sim** — passa a excluir os já vencidos |
| `DashboardPage.tsx` — histograma que alimenta o donut (`statusCounts`) | agrupa pelo status bruto | **Sim** — buckets "Pendente" e "Outros" passam a refletir vencidos (o bucket "Outros" já soma `EXPIRADO`, só nunca recebeu nada porque nada nunca era essa string) |
| `DashboardPage.tsx` — KPI "Aprovados" + valor aprovado | filtra por `APROVADO` bruto | Não (Aprovado nunca expira pela regra) — troca por consistência/segurança futura |
| `DashboardPage.tsx` — linha "aprovados últimos 6 meses" | filtra por `APROVADO` bruto | Não — mesma razão |
| `OrcamentoDetailPage.tsx` — badge de status | exibe status bruto | **Sim** — mostra o calculado |
| `OrcamentosPage.tsx` — badge na listagem | exibe status bruto | **Sim** — mostra o calculado (não há filtro por status nessa tela, só a badge por linha) |
| `OrcamentoFormPage.tsx` — dropdown de edição de status | lê valor inicial e grava ao salvar | **Fica no bruto, deliberadamente** — é o único lugar que manipula o valor real armazenado; usar o calculado faria o formulário achar que "Expirado" foi uma escolha manual e gravar isso como se fosse |

`StatusBadge.tsx` e `statusLabel()` (`formatters.ts`) não precisam mudar — são só apresentação, recebem a string que o chamador decidir passar.

## 4. Casos de borda

- Orçamento sem `dataEmissao` ou sem `validadeDias` (não deveria acontecer — ambos obrigatórios na validação atual — mas defensivamente): `dataVencimento()` retorna `null`, `statusExibicao` cai no `else` e devolve o status bruto sem tentar comparar com `null`.
- Um orçamento já com `statusOrcamento = EXPIRADO` definido manualmente (hoje não alcançável pela UI, mas o enum permite): a regra só reescreve `SOLICITADO`/`ENVIADO`, então um `EXPIRADO` manual passa direto sem ser reavaliado — idempotente.

## 5. Testes

Backend, seguindo o padrão já existente (`OrcamentoServiceTest`, JUnit + Mockito + AssertJ):

- Orçamento `SOLICITADO` com vencimento no passado → `statusExibicao` = `EXPIRADO`.
- Orçamento `ENVIADO` com vencimento no futuro → `statusExibicao` = `ENVIADO` (inalterado).
- Orçamento `APROVADO` com vencimento no passado → `statusExibicao` = `APROVADO` (regra não se aplica).
- Orçamento `REJEITADO` / `RASCUNHO` com vencimento no passado → status inalterado.
- Em todos os casos acima, `statusOrcamento` (campo bruto) nunca é modificado pelo cálculo — assert no valor do campo em si, não só no retorno do getter.

Frontend: sem infra de teste além da base plantada no bloco de Autenticação (ver spec correspondente) — verificação manual dos 6 pontos da tabela da seção 3.3.

## 6. Fora de escopo

- Remover o valor `VENCIDO` do enum.
- Persistir o status calculado no banco / job agendado.
- Popular ou passar a usar o campo `dataValidade`.
