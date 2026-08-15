# Design: Documento gerado — projeto, imagens, tipografia e resumo financeiro

**Data:** 2026-08-15
**Status:** Aprovado
**Repositórios afetados:** `marmoraria-orcamentos-api` (template, CSS e service — a maior parte) + `marmoraria-orcamentos-web` (só a divisão do checkbox de projeto em dois)
**Itens do backlog:** 4, 5, 6 e 7 — agrupados numa spec só porque vivem nos mesmos arquivos: `OrcamentoDocumentoService.java` e os templates/CSS em `src/main/resources/templates/orcamento/`.

---

## 1. Informações do projeto (item 4)

### 1.1 Problema

`projeto.html` sempre renderiza a seção "Dados do Projeto" com os 4 campos (nome, pedra principal, responsável técnico, observações), caindo em "-" ou texto de placeholder ("Projeto", "Tipo de pedra a definir") quando vazios, em vez de simplesmente não aparecer. Quando não há nenhuma imagem, `imagemOuPlaceholder()` desenha um quadrado cinza grande no lugar. Hoje uma única flag (`imprimirProjeto`) controla texto e imagens juntos — não dá pra esconder um sem o outro.

### 1.2 Decisão

- Duas flags independentes em vez de uma.
- Cada campo de texto some individualmente quando vazio (mesmo padrão já usado em `camposClienteExtras()`/`infoBlockSe()` para os campos extras do cliente). Se os 4 estiverem vazios, a seção inteira — cabeçalho "Dados do Projeto" incluído — some.
- Bloco de imagens some inteiro (cabeçalho "Visualização do Projeto" incluído) quando não há nenhuma imagem — não cai mais no placeholder cinza.

### 1.3 Design

`OpcoesGeracaoRequest`: o campo `imprimirProjeto` é renomeado para `imprimirProjetoTexto`; novo campo `imprimirProjetoImagens`. `isImprimirProjetoImagensAtivo()` segue o mesmo padrão de default (`true` quando não informado) que `isImprimirProjetoAtivo()` já usa.

`paginaProjeto()` divide em duas chamadas independentes, cada uma controlada pela sua flag:

- **Texto:** monta os `info-block` só para campos não-vazios (mesmo padrão de `infoBlockSe`). Se nenhum campo tiver conteúdo, retorna string vazia — a seção inteira, com cabeçalho, some.
- **Imagens:** se `imagensProjetoHtml()` não encontrar nenhuma imagem real (nem galeria, nem foto principal), retorna string vazia em vez de cair no placeholder.

`projeto.html` é dividido em dois templates (ex.: `projeto-info.html` + `projeto-galeria.html`), já que hoje é um único arquivo cobrindo as duas seções — cada um renderizado (ou omitido) independentemente a partir de `identificacao.html`.

**Frontend:** `GerarOrcamentoOptions` (`api.types.ts`) ganha o segundo campo (`ocultarProjetoImagens`, ao lado do já existente `ocultarProjeto`, possivelmente renomeado para `ocultarProjetoTexto` por clareza); `OrcamentoDetailPage.tsx` ganha o segundo checkbox; `toBackendPayload()` em `orcamentos.service.ts` mapeia os dois valores.

## 2. Imagens cortadas (item 5)

### 2.1 Problema confirmado

O corte acontece só no template do PDF, via CSS — não no cadastro nem na visualização. `object-fit: cover` em `.gallery-grid__item` e `.gallery-grid__item--wide` (`orcamento.css`) força toda foto do projeto numa caixa de proporção fixa (4:3 ou 16:7) e corta o que não couber, incluindo medidas anotadas nas laterais da imagem. O upload no Cloudinary não aplica nenhuma transformação (`CloudinaryService.salvar()` chama `cloudinary.uploader().upload(bytes, Map.of())`, com mapa de transformação vazio), e o app React não chega a exibir preview de imagem hoje (só um chip com o nome do arquivo) — então não há corte antes do PDF.

### 2.2 Decisão

Trocar `object-fit: cover` por `object-fit: contain` nas duas classes de galeria (`.gallery-grid__item`, `.gallery-grid__item--wide`). Mostra a imagem inteira, sem cortar nada; pode sobrar uma faixa vazia nas laterais ou em cima/embaixo quando a proporção da foto não bate com a da caixa — trade-off aceito em troca de nunca perder conteúdo da imagem.

`.product-thumb` (o ícone de 34×34px na tabela de itens) fica **fora** dessa mudança — é um thumbnail pequeno onde medidas anotadas na foto não são o ponto, e mudar geraria inconsistência visual na tabela sem resolver o problema relatado (que é sobre as fotos do projeto, maiores).

## 3. Tipografia e espaçamento (item 6)

### 3.1 Problema confirmado

`.product-desc` (descrição do item na tabela de produtos) está em **7.5px**, bem abaixo de `.product-name` (9px) e do corpo do documento (12px, via `--paragraph-size`).

### 3.2 Decisão

Subir `.product-desc` para ~9.5px. Para compensar o crescimento da altura da linha, aperta-se ligeiramente o padding de `.products-table tbody td` (hoje 8px) e a margem inferior de `.product-name` — valores exatos ajustados durante a implementação, olhando um PDF de exemplo gerado de verdade, não fechados a priori além do valor do `font-size`, que é o ponto explícito do pedido.

## 4. Total vazando no resumo financeiro (item 7)

### 4.1 Problema confirmado

Em `totais.html`, dentro da seção "Condições de Pagamento", existe um bloco "Resumo financeiro" que embute a tabela de totais inteira (placeholder `[[LINHAS_TOTAIS]]`). Em `paginaTotais()` (`OrcamentoDocumentoService.java`), só a linha "Total final" dentro dessa tabela é condicionada à flag `imprimirTotal` — "Subtotal dos itens" (e frete/desconto/adendos, quando existentes) sempre entra, incondicionalmente. Quando não há desconto nem frete, subtotal e total final são o mesmo número — esconder só a linha "Total final" não esconde o valor, que reaparece do lado como "Subtotal dos itens".

Existe uma segunda cópia dessa mesma tabela de totais, no rodapé da lista de itens (`tfootTotais()`), já corrigida para respeitar a flag no commit `957ed7a`. A cópia dentro de "Condições de Pagamento" ficou de fora daquela correção.

### 4.2 Decisão

O bloco "Resumo financeiro" inteiro (o `<li class="payment-item payment-item--full">` em `totais.html`) passa a ser condicionado à mesma flag `imprimirTotal` que já esconde a linha "Total final" — não só a linha, o bloco todo, incluindo o rótulo "Resumo financeiro" e a linha "Subtotal dos itens".

### 4.3 Design

`paginaTotais()` monta o `<li>` do resumo financeiro como um fragmento próprio (novo placeholder, ex. `RESUMO_FINANCEIRO_BLOCO`), populado só quando `opcoes.isImprimirTotalAtivo()` for verdadeiro; `totais.html` troca o HTML fixo desse `<li>` pelo placeholder único. Com a flag desligada, o bloco some por completo.

## 5. Testes

Backend, estendendo o padrão de `OrcamentoServiceTest` para `OrcamentoDocumentoService` (JUnit, asserções sobre a string HTML retornada por `gerarHtml()`):

- Projeto com todos os campos de texto vazios → HTML não contém a seção "Dados do Projeto".
- Projeto com só o campo "nome" preenchido → HTML contém "Nome do Projeto" mas não "Material / Ambiente", "Responsável Técnico" nem "Observações do Projeto".
- Projeto sem nenhuma imagem → HTML não contém a seção "Visualização do Projeto" nem a classe `image-placeholder`.
- `ocultarTotalGeral` ativo → HTML não contém o valor formatado do total em nenhuma das duas ocorrências (rodapé da tabela de itens **e** bloco de Condições de Pagamento).
- `ocultarTotalGeral` desativado → ambas as ocorrências aparecem normalmente.

**Verificação visual:** gerar um PDF de exemplo real (via `POST /api/orcamento/{id}/gerar` contra um orçamento de teste) para conferir o resultado de tipografia/espaçamento (item 6) e o enquadramento das imagens (item 5) antes de considerar esse bloco concluído — são ajustes visuais que valem mais uma checagem de olho do que uma asserção de pixel.

## 6. Fora de escopo

- Mudar `.product-thumb` (thumbnail da tabela de itens).
- Reativar `paginaResumo()`/`resumo.html` — código morto de um modo de documento já removido (commit `9407295`), sem relação com este pedido.
- Qualquer alteração na página de capa, avisos, ou observações.
