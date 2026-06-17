package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.dto.GerarOrcamentoRequest;
import com.marmoraria.orcamentos.dto.OpcoesGeracaoRequest;
import com.marmoraria.orcamentos.entity.Cliente;
import com.marmoraria.orcamentos.entity.Financeiro;
import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.entity.Projeto;
import com.marmoraria.orcamentos.entity.ProjetoImagem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class OrcamentoDocumentoService {

    private static final String TEMPLATE_BASE = "templates/orcamento/";
    private static final String ASSET_BASE = "templates/orcamento/html-orcamentos-api/html-orcamentos-api/assets/";

    private static final String EMPRESA_NOME = "Gaúcha Mármores e Granitos";
    private static final String EMPRESA_SLOGAN = "Pedras que transformam";
    private static final String EMPRESA_ENDERECO = "RS 307 KM 01 (TREVO)";
    private static final String EMPRESA_BAIRRO = "Campina das Missões";
    private static final String EMPRESA_CIDADE = "Campina das Missões";
    private static final String EMPRESA_ESTADO = "RS";
    private static final String EMPRESA_CEP = "-";
    private static final String EMPRESA_CNPJ = "-";
    private static final String EMPRESA_INSCRICAO_ESTADUAL = "-";
    private static final String EMPRESA_TELEFONE = "(55) 99651-5484";
    private static final String EMPRESA_INSTAGRAM = "@gauchamarmoresegranitos";
    private static final String EMPRESA_EMAIL = "gauchamarmoresegranitos@hotmail.com";
    private static final String FRASE_CAPA = "SEU PROJETO COMEÇA NA ESCOLHA CERTA.";

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");

    @Autowired
    private OrcamentoService orcamentoService;

    @Transactional(readOnly = true)
    public String gerarHtml(Long orcamentoId, GerarOrcamentoRequest request) {
        Orcamento orcamento = orcamentoService.buscarPorId(orcamentoId);
        orcamentoService.calcularValorTotal(orcamento);

        OpcoesGeracaoRequest opcoes = normalizarOpcoes(request);
        String responsavelTecnico = responsavelTecnico(orcamento);

        Map<String, String> dados = contextoBase();
        dados.put("CSS_DOCUMENTO", renderizar("orcamento.css", contextoBase()));
        dados.put("TITULO_DOCUMENTO", "Orçamento " + esc(numeroOrcamento(orcamento)));
        dados.put("CABECALHO_GLOBAL", cabecalho(orcamento));
        dados.put("RODAPE_GLOBAL", rodape());
        dados.put("PAGINA_CAPA", opcoes.isImprimirCapaAtivo() ? paginaCapa(orcamento) : "");
        dados.put("PAGINA_RESUMO", "");
        dados.put("PAGINA_IDENTIFICACAO", paginaIdentificacao(orcamento, responsavelTecnico, opcoes));
        dados.put("PAGINA_PROJETO", "");
        dados.put("PAGINA_AVISOS", "");
        dados.put("PAGINA_ITENS", paginaItens(orcamento, opcoes));
        String obsDocumento = (request != null && !isBlank(request.getObservacoesDocumento()))
                ? request.getObservacoesDocumento() : null;
        dados.put("PAGINA_TOTAIS", paginaTotais(orcamento, opcoes, responsavelTecnico, obsDocumento));

        return renderizar("orcamento.html", dados);
    }

    public byte[] gerarPdf(Long orcamentoId, GerarOrcamentoRequest request) {
        String html = gerarHtml(orcamentoId, request);
        try {
            return gerarPdfComNavegador(html);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar PDF: " + e.getMessage(), e);
        }
    }

    private byte[] gerarPdfComNavegador(String html) throws IOException, InterruptedException {
        String navegador = navegadorDisponivel();
        if (navegador == null) {
            throw new IllegalStateException("Nenhum navegador compativel encontrado");
        }

        Path pastaTemporaria = Files.createTempDirectory("orcamento-pdf-");
        Path arquivoHtml = pastaTemporaria.resolve("orcamento.html");
        Path arquivoPdf = pastaTemporaria.resolve("orcamento.pdf");
        Files.writeString(arquivoHtml, html, StandardCharsets.UTF_8);

        try {
            try {
                executarNavegadorPdf(navegador, arquivoHtml, arquivoPdf, "--headless=new");
            } catch (IllegalStateException exception) {
                executarNavegadorPdf(navegador, arquivoHtml, arquivoPdf, "--headless");
            }
            if (!Files.exists(arquivoPdf)) {
                throw new IllegalStateException("Navegador nao gerou o arquivo PDF");
            }

            return Files.readAllBytes(arquivoPdf);
        } finally {
            apagarTemporario(arquivoPdf);
            apagarTemporario(arquivoHtml);
            apagarTemporario(pastaTemporaria);
        }
    }

    private void apagarTemporario(Path caminho) {
        try {
            Files.deleteIfExists(caminho);
        } catch (IOException ignored) {
            // Arquivo temporario nao deve impedir o retorno do PDF ja gerado.
        }
    }

    private void executarNavegadorPdf(String navegador, Path arquivoHtml, Path arquivoPdf, String modoHeadless) throws IOException, InterruptedException {
        Files.deleteIfExists(arquivoPdf);

        List<String> comando = new ArrayList<>();
        comando.add(navegador);
        comando.add(modoHeadless);
        comando.add("--no-sandbox");
        comando.add("--disable-setuid-sandbox");
        comando.add("--no-zygote");
        comando.add("--disable-gpu");
        comando.add("--disable-dev-shm-usage");
        comando.add("--disable-extensions");
        comando.add("--run-all-compositor-stages-before-draw");
        comando.add("--virtual-time-budget=1000");
        comando.add("--allow-file-access-from-files");
        comando.add("--no-pdf-header-footer");
        comando.add("--print-to-pdf-no-header");
        comando.add("--print-to-pdf=" + arquivoPdf.toAbsolutePath());
        comando.add(arquivoHtml.toUri().toString());

        Process processo = new ProcessBuilder(comando).redirectErrorStream(true).start();
        boolean finalizado = processo.waitFor(40, TimeUnit.SECONDS);
        String saida = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!finalizado) {
            processo.destroyForcibly();
            throw new IllegalStateException("Tempo excedido ao gerar PDF pelo navegador");
        }
        if (processo.exitValue() != 0 || !Files.exists(arquivoPdf)) {
            throw new IllegalStateException("Navegador nao gerou o PDF. Saida: " + saida);
        }
    }

    private String navegadorDisponivel() {
        // Permite sobrescrever via variável de ambiente (ex: Railway com caminho customizado)
        String envPath = System.getenv("BROWSER_PATH");
        if (envPath != null && !envPath.isBlank() && Files.exists(Path.of(envPath))) {
            return envPath;
        }

        List<String> candidatos = List.of(
                // Linux — Railway / Docker
                "/usr/bin/google-chrome-stable",
                "/usr/bin/google-chrome",
                "/usr/bin/chromium-browser",
                "/usr/bin/chromium",
                "/snap/bin/chromium",
                // Windows
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe"
        );
        for (String candidato : candidatos) {
            if (Files.exists(Path.of(candidato))) {
                return candidato;
            }
        }

        // Tenta localizar via PATH do sistema (Linux/Nix — "which" pode não existir)
        String[] buscas = {"chromium", "chromium-browser", "google-chrome-stable", "google-chrome"};
        for (String bin : buscas) {
            try {
                Process p = new ProcessBuilder("bash", "-c", "command -v " + bin)
                        .redirectErrorStream(true).start();
                if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    String resultado = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (!resultado.isBlank()) return resultado;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    public String nomeArquivoPdf(Long orcamentoId) {
        Orcamento orcamento = orcamentoService.buscarPorId(orcamentoId);
        return "orcamento-" + numeroOrcamento(orcamento).replaceAll("[^A-Za-z0-9-]", "") + ".pdf";
    }

    private OpcoesGeracaoRequest normalizarOpcoes(GerarOrcamentoRequest request) {
        if (request == null || request.getOpcoes() == null) {
            return OpcoesGeracaoRequest.padrao();
        }
        OpcoesGeracaoRequest opcoes = request.getOpcoes();
        if (opcoes.getImprimirCapa() == null) {
            opcoes.setImprimirCapa(false);
        }
        if (opcoes.getImprimirTotal() == null) {
            opcoes.setImprimirTotal(true);
        }
        return opcoes;
    }


    private String paginaCapa(Orcamento orcamento) {
        Map<String, String> dados = contextoBase();
        dados.put("NOME_CLIENTE", esc(nomeCliente(orcamento)));
        dados.put("NUMERO_ORCAMENTO", esc(numeroOrcamento(orcamento)));
        dados.put("DATA_EMISSAO", esc(formatarData(orcamento.getDataEmissao())));
        dados.put("FRASE_CAPA", esc(FRASE_CAPA));
        dados.put("LOGO_BRANCA_SRC", assetDataUri("Logo_ofc_white.png"));
        return renderizar("capa.html", dados);
    }

    private String paginaResumo(Orcamento orcamento) {
        Financeiro financeiro = financeiroOuVazio(orcamento);
        Projeto projeto = orcamento.getProjeto();
        LocalDate vencimento = dataVencimento(orcamento);

        Map<String, String> dados = contextoBase();
        dados.put("SECTION_CLASS", "doc-section-inline");
        dados.put("NOME_CLIENTE", esc(nomeCliente(orcamento)));

        // Linha do projeto (somente se tiver nome ou material)
        String nomeProjeto = projeto == null ? null : projeto.getNome();
        String material = projeto == null ? null : projeto.getTipoPedraPrincipal();
        if (!isBlank(nomeProjeto) || !isBlank(material)) {
            StringBuilder linha = new StringBuilder("<p class=\"resumo-projeto\">");
            if (!isBlank(nomeProjeto)) linha.append("<strong>").append(esc(nomeProjeto)).append("</strong>");
            if (!isBlank(nomeProjeto) && !isBlank(material)) linha.append(" &nbsp;·&nbsp; ");
            if (!isBlank(material)) linha.append(esc(material));
            linha.append("</p>");
            dados.put("RESUMO_PROJETO_LINHA", linha.toString());
        } else {
            dados.put("RESUMO_PROJETO_LINHA", "");
        }

        // Cards: material e prazo (omite se vazio)
        dados.put("RESUMO_CARD_MATERIAL", isBlank(material) ? "" :
            "<div class=\"resumo-card\"><span class=\"resumo-card__label\">Material Principal</span>" +
            "<span class=\"resumo-card__value\">" + esc(material) + "</span></div>");

        String prazo = orcamento.getPrazoExecucaoDias() == null ? null :
            orcamento.getPrazoExecucaoDias() + " dias úteis";
        dados.put("RESUMO_CARD_PRAZO", isBlank(prazo) ? "" :
            "<div class=\"resumo-card\"><span class=\"resumo-card__label\">Prazo de Produção</span>" +
            "<span class=\"resumo-card__value\">" + esc(prazo) + "</span>" +
            "<span class=\"resumo-card__sub\">após aprovação e entrada</span></div>");

        dados.put("DATA_VENCIMENTO", esc(formatarData(vencimento)));
        dados.put("VALIDADE_DIAS", esc(validadeTexto(orcamento)));

        // Bloco de valor — exibe total geral (subtotal antes de descontos)
        dados.put("TOTAL_GERAL", esc(formatarMoeda(financeiro.getSubtotalItens())));
        if (valorOuZero(financeiro.getDescontoValorReais()).compareTo(BigDecimal.ZERO) > 0) {
            dados.put("RESUMO_DESCONTO",
                "<span class=\"resumo-valor__desconto\">Desconto: -" +
                esc(formatarMoeda(financeiro.getDescontoValorReais())) +
                " &nbsp;·&nbsp; Total Final: " +
                esc(formatarMoeda(financeiro.getTotalFinal())) + "</span>");
        } else {
            dados.put("RESUMO_DESCONTO", "");
        }

        // Bloco de pagamento
        String meioPagTitulo = financeiro.getMeioPagamentoTitulo();
        String meioPagDesc = financeiro.getMeioPagamentoDescricao();
        if (!isBlank(meioPagTitulo)) {
            StringBuilder pag = new StringBuilder("<div class=\"resumo-pagamento\">")
                .append("<span class=\"resumo-pagamento__label\">Forma de Pagamento</span>")
                .append("<span class=\"resumo-pagamento__titulo\">").append(esc(meioPagTitulo)).append("</span>");
            if (!isBlank(meioPagDesc)) {
                pag.append("<span class=\"resumo-pagamento__desc\">").append(esc(meioPagDesc)).append("</span>");
            }
            pag.append("</div>");
            dados.put("RESUMO_PAGAMENTO", pag.toString());
        } else {
            dados.put("RESUMO_PAGAMENTO", "");
        }

        return renderizar("resumo.html", dados);
    }

    private String paginaIdentificacao(Orcamento orcamento, String responsavelTecnico, OpcoesGeracaoRequest opcoes) {
        Cliente cliente = orcamento.getCliente();
        LocalDate vencimento = dataVencimento(orcamento);

        Map<String, String> dados = contextoBase();
        dados.put("SECTION_CLASS", "doc-section-inline");
        dados.put("NUMERO_ORCAMENTO", esc(numeroOrcamento(orcamento)));
        dados.put("NOME_CLIENTE", esc(nomeCliente(orcamento)));
        dados.put("CAMPOS_CLIENTE_EXTRAS", camposClienteExtras(cliente));
        dados.put("DATA_EMISSAO", esc(formatarData(orcamento.getDataEmissao())));
        dados.put("VALIDADE_DIAS", esc(validadeTexto(orcamento)));
        dados.put("DATA_VENCIMENTO", esc(formatarData(vencimento)));
        dados.put("SECAO_PROJETO", opcoes.isImprimirProjetoAtivo() ? paginaProjeto(orcamento, responsavelTecnico) : "");

        return renderizar("identificacao.html", dados);
    }

    private String camposClienteExtras(Cliente cliente) {
        StringBuilder sb = new StringBuilder();
        String cpf = cliente != null ? cliente.getCpfCnpj() : null;
        String endereco = cliente != null ? cliente.getEndereco() : null;
        String cidade = cliente != null ? cliente.getCidade() : null;
        String telefone = cliente != null ? cliente.getTelefone() : null;
        String email = cliente != null ? cliente.getEmail() : null;
        infoBlockSempre(sb, "CPF / CNPJ", cpf);
        infoBlockSe(sb, "Endereço", endereco);
        infoBlockSe(sb, "Cidade / UF", cidade);
        infoBlockSempre(sb, "Telefone", telefone);
        infoBlockSe(sb, "E-mail", email);
        return sb.toString();
    }

    private void infoBlockSempre(StringBuilder sb, String label, String value) {
        sb.append("<div class=\"info-block\">")
          .append("<span class=\"info-block__label\">").append(esc(label)).append("</span>")
          .append("<span class=\"info-block__value\">").append(isBlank(value) ? "&nbsp;" : esc(value)).append("</span>")
          .append("</div>");
    }

    private void infoBlockSe(StringBuilder sb, String label, String value) {
        if (isBlank(value)) return;
        sb.append("<div class=\"info-block\">")
          .append("<span class=\"info-block__label\">").append(esc(label)).append("</span>")
          .append("<span class=\"info-block__value\">").append(esc(value)).append("</span>")
          .append("</div>");
    }

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

    private String paginaItens(Orcamento orcamento, OpcoesGeracaoRequest opcoes) {
        StringBuilder itensHtml = new StringBuilder();
        List<ItemOrcamento> itens = orcamento.getItemOrcamentoList() == null ? List.of() : orcamento.getItemOrcamentoList();

        for (ItemOrcamento item : itens) {
            Map<String, String> dadosItem = contextoBase();
            dadosItem.put("CODIGO_ITEM", esc(codigoItem(item)));
            dadosItem.put("IMAGEM_ITEM", imagemOuPlaceholder(imagemItem(item), "product-thumb", "Imagem do item", "Sem imagem"));
            dadosItem.put("NOME_ITEM", esc(nomeItem(item)));
            dadosItem.put("DESCRICAO_ITEM", esc(descricaoItem(item)));
            dadosItem.put("UNIDADE_ITEM", esc(unidadeItem(item)));
            dadosItem.put("QUANTIDADE", esc(valor(item.getQuantidade())));
            dadosItem.put("PRECO_UNITARIO", esc(formatarMoeda(item.getPrecoUnitario())));
            dadosItem.put("SUBTOTAL_ITEM", esc(formatarMoeda(item.getValorTotal())));
            itensHtml.append(renderizar("item.html", dadosItem));
        }

        Financeiro financeiro = financeiroOuVazio(orcamento);
        Map<String, String> dados = contextoBase();
        dados.put("SECTION_CLASS", "doc-section-inline");
        dados.put("ITENS_ORCAMENTO", itensHtml.isEmpty() ? linhaTabelaVazia("Nenhum item cadastrado neste orçamento.") : itensHtml.toString());
        dados.put("DESCONTO_VALOR", esc(formatarMoeda(financeiro.getDescontoValorReais())));
        dados.put("TOTAL_FINAL", esc(formatarMoeda(financeiro.getTotalFinal())));

        return renderizar("itens.html", dados);
    }

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

    private String meioPagamentoItem(Financeiro financeiro) {
        String titulo = financeiro.getMeioPagamentoTitulo();
        String descricao = financeiro.getMeioPagamentoDescricao();
        if (isBlank(titulo)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<li class=\"payment-item\">");
        sb.append("<span class=\"payment-item__label\">").append(esc(titulo)).append("</span>");
        if (!isBlank(descricao)) {
            sb.append("<span class=\"payment-item__value\">").append(esc(descricao)).append("</span>");
        }
        sb.append("</li>");
        return sb.toString();
    }

    private String responsavelTecnico(Orcamento orcamento) {
        Projeto projeto = orcamento == null ? null : orcamento.getProjeto();
        if (projeto != null && !isBlank(projeto.getResponsavelTecnico())) {
            return projeto.getResponsavelTecnico();
        }
        return "";
    }

    private String cabecalho(Orcamento orcamento) {
        Map<String, String> dados = contextoBase();
        dados.put("NUMERO_ORCAMENTO", esc(numeroOrcamento(orcamento)));
        dados.put("DATA_EMISSAO", esc(formatarData(orcamento.getDataEmissao())));
        dados.put("DATA_VENCIMENTO", esc(formatarData(dataVencimento(orcamento))));
        dados.put("VALIDADE_DIAS", esc(validadeTexto(orcamento)));
        dados.put("LOGO_COLORIDA_SRC", assetDataUri("Logo_ofc.png"));
        return renderizar("cabecalho.html", dados);
    }

    private String linhaTotal(String label, String valor, boolean destaque) {
        Map<String, String> dados = contextoBase();
        dados.put("CLASSE_LINHA_TOTAL", destaque ? "highlight" : "");
        dados.put("LABEL_TOTAL", esc(label));
        dados.put("VALOR_TOTAL", esc(valor));
        return renderizar("linha-total.html", dados);
    }

    private Map<String, String> contextoBase() {
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("EMPRESA_NOME", esc(EMPRESA_NOME));
        dados.put("EMPRESA_SLOGAN", esc(EMPRESA_SLOGAN));
        dados.put("EMPRESA_ENDERECO", esc(EMPRESA_ENDERECO));
        dados.put("EMPRESA_BAIRRO", esc(EMPRESA_BAIRRO));
        dados.put("EMPRESA_CIDADE", esc(EMPRESA_CIDADE));
        dados.put("EMPRESA_ESTADO", esc(EMPRESA_ESTADO));
        dados.put("EMPRESA_CEP", esc(EMPRESA_CEP));
        dados.put("EMPRESA_CNPJ", esc(EMPRESA_CNPJ));
        dados.put("EMPRESA_INSCRICAO_ESTADUAL", esc(EMPRESA_INSCRICAO_ESTADUAL));
        dados.put("EMPRESA_TELEFONE", esc(EMPRESA_TELEFONE));
        dados.put("EMPRESA_INSTAGRAM", esc(EMPRESA_INSTAGRAM));
        dados.put("EMPRESA_EMAIL", esc(EMPRESA_EMAIL));
        dados.put("CAPA_PRINCIPAL_SRC", assetDataUri("capa-principal.png"));
        return dados;
    }

    private String renderizar(String nomeTemplate, Map<String, String> dados) {
        String template = carregarTemplate(nomeTemplate);
        for (Map.Entry<String, String> entrada : dados.entrySet()) {
            template = template.replace("[[" + entrada.getKey() + "]]", entrada.getValue() == null ? "" : entrada.getValue());
        }
        return template;
    }

    private String carregarTemplate(String nomeTemplate) {
        String caminho = TEMPLATE_BASE + nomeTemplate;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(caminho)) {
            if (inputStream == null) {
                throw new IllegalStateException("Template do orçamento não encontrado: " + caminho);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler o template do orçamento: " + caminho, exception);
        }
    }

    private String rodape() {
        Map<String, String> dados = contextoBase();
        return renderizar("rodape.html", dados);
    }

    private String imagemOuPlaceholder(String url, String classeImagem, String alt, String textoPlaceholder) {
        if (isBlank(url)) {
            return "<div class=\"" + classeImagem + " image-placeholder\">" + esc(textoPlaceholder) + "</div>";
        }
        return "<img class=\"" + classeImagem + "\" src=\"" + esc(url) + "\" alt=\"" + esc(alt) + "\" />";
    }

    private String estadoVazio(String mensagem) {
        return "<div class=\"empty-state\">" + esc(mensagem) + "</div>";
    }

    private String linhaTabelaVazia(String mensagem) {
        return "<tr><td colspan=\"6\" class=\"empty-table\">" + esc(mensagem) + "</td></tr>";
    }

    private Financeiro financeiroOuVazio(Orcamento orcamento) {
        return orcamento.getFinanceiro() == null ? new Financeiro() : orcamento.getFinanceiro();
    }

    private String codigoItem(ItemOrcamento item) {
        if (item.getCod() != null) {
            return String.valueOf(item.getCod());
        }
        if (item.getProdutoServico() != null && item.getProdutoServico().getId() != null) {
            return String.valueOf(item.getProdutoServico().getId());
        }
        return valor(item.getId());
    }

    private String nomeItem(ItemOrcamento item) {
        if (!isBlank(item.getNome())) {
            return item.getNome();
        }
        if (item.getProdutoServico() != null && !isBlank(item.getProdutoServico().getNome())) {
            return item.getProdutoServico().getNome();
        }
        return "Item " + valor(item.getId());
    }

    private String descricaoItem(ItemOrcamento item) {
        List<String> partes = new ArrayList<>();
        adicionarSePreenchido(partes, item.getDescricao());
        adicionarSePreenchido(partes, item.getTipoPedra());
        adicionarSePreenchido(partes, item.getAcabamento());
        adicionarSePreenchido(partes, item.getDimensoes());
        return partes.isEmpty() ? "-" : String.join(" | ", partes);
    }

    private String unidadeItem(ItemOrcamento item) {
        if (item.getProdutoServico() != null && item.getProdutoServico().getUnidadeMedida() != null) {
            return item.getProdutoServico().getUnidadeMedida().name();
        }
        if (item.getMetrosQuadrados() != null && item.getMetrosQuadrados().compareTo(BigDecimal.ZERO) > 0) {
            return "M2";
        }
        return "UN";
    }

    private String imagemItem(ItemOrcamento item) {
        if (item.getImagens() != null && !item.getImagens().isEmpty() && !isBlank(item.getImagens().get(0).getUrl())) {
            return item.getImagens().get(0).getUrl();
        }
        if (!isBlank(item.getImagemUrl())) {
            return item.getImagemUrl();
        }
        if (item.getProdutoServico() != null) {
            if (!isBlank(item.getProdutoServico().getImagemUrl())) {
                return item.getProdutoServico().getImagemUrl();
            }
            if (!isBlank(item.getProdutoServico().getImagemPath())) {
                return item.getProdutoServico().getImagemPath();
            }
        }
        return null;
    }

    private void adicionarSePreenchido(List<String> partes, String valor) {
        if (!isBlank(valor)) {
            partes.add(valor);
        }
    }

    private String validadeTexto(Orcamento orcamento) {
        if (orcamento.getValidadeDias() != null) {
            return String.valueOf(orcamento.getValidadeDias());
        }
        if (orcamento.getDataEmissao() != null && orcamento.getDataValidade() != null) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(orcamento.getDataEmissao(), orcamento.getDataValidade());
            if (dias > 0) {
                return String.valueOf(dias);
            }
        }
        return "-";
    }

    private String prazoProducao(Orcamento orcamento) {
        if (orcamento.getPrazoExecucaoDias() == null) {
            return "-";
        }
        return orcamento.getPrazoExecucaoDias() + " dias úteis após aprovação.";
    }

    private LocalDate dataVencimento(Orcamento orcamento) {
        if (orcamento.getDataEmissao() != null && orcamento.getValidadeDias() != null) {
            return orcamento.getDataEmissao().plusDays(orcamento.getValidadeDias());
        }
        return orcamento.getDataValidade();
    }

    private String numeroOrcamento(Orcamento orcamento) {
        if (!isBlank(orcamento.getNumero())) {
            return orcamento.getNumero();
        }
        return orcamento.getCod() == null ? String.valueOf(orcamento.getId()) : String.valueOf(orcamento.getCod());
    }

    private String nomeCliente(Orcamento orcamento) {
        return orcamento.getCliente() == null ? "Cliente" : valor(orcamento.getCliente().getNome());
    }

    private String cidadeCliente(Cliente cliente) {
        if (cliente == null) {
            return "-";
        }
        if (!isBlank(cliente.getCidade())) {
            return cliente.getCidade();
        }
        return "-";
    }

    private String formatarData(LocalDate data) {
        return data == null ? "-" : DATA_FORMATTER.format(data);
    }

    private String formatarMoeda(BigDecimal valor) {
        NumberFormat format = NumberFormat.getCurrencyInstance(LOCALE_BR);
        return format.format(valorOuZero(valor));
    }

    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private String assetDataUri(String nomeArquivo) {
        String caminho = ASSET_BASE + nomeArquivo;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(caminho)) {
            if (inputStream == null) {
                return "";
            }
            String mimeType = nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".jpg") || nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".jpeg")
                    ? "image/jpeg"
                    : "image/png";
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(inputStream.readAllBytes());
        } catch (IOException exception) {
            return "";
        }
    }

    private String valor(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String obsDocumentoItens(String texto) {
        if (isBlank(texto)) return "";
        StringBuilder sb = new StringBuilder();
        for (String linha : texto.split("\n")) {
            String limpa = linha.strip();
            if (!limpa.isEmpty()) {
                sb.append("<li>").append(esc(limpa)).append("</li>");
            }
        }
        return sb.toString();
    }

    /** Retorna true somente se o título for um texto descritivo, não um nome de arquivo. */
    private boolean tituloValido(String titulo) {
        if (isBlank(titulo)) return false;
        String lower = titulo.toLowerCase(Locale.ROOT);
        return !lower.matches(".*\\.(jpg|jpeg|png|webp|gif|bmp|tiff|svg)$");
    }

    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
