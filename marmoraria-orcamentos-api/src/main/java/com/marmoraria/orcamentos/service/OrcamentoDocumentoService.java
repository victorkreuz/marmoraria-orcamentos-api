package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.dto.GerarOrcamentoRequest;
import com.marmoraria.orcamentos.dto.OpcoesGeracaoRequest;
import com.marmoraria.orcamentos.entity.Cliente;
import com.marmoraria.orcamentos.entity.Financeiro;
import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.entity.ObservacaoOrcamento;
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

    @Autowired
    private ObservacaoOrcamentoService observacaoService;

    @Transactional(readOnly = true)
    public String gerarHtml(Long orcamentoId, GerarOrcamentoRequest request) {
        Orcamento orcamento = orcamentoService.buscarPorId(orcamentoId);
        orcamentoService.calcularValorTotal(orcamento);

        OpcoesGeracaoRequest opcoes = normalizarOpcoes(request);
        List<ObservacaoOrcamento> observacoes = resolverObservacoes(orcamento, request);
        String responsavelTecnico = responsavelTecnico(orcamento);

        Map<String, String> dados = contextoBase();
        dados.put("CSS_DOCUMENTO", renderizar("orcamento.css", contextoBase()));
        dados.put("TITULO_DOCUMENTO", "Orçamento " + esc(numeroOrcamento(orcamento)));
        dados.put("PAGINA_CAPA", opcoes.isImprimirCapaAtivo() ? paginaCapa(orcamento) : "");
        dados.put("PAGINA_IDENTIFICACAO", paginaIdentificacao(orcamento, opcoes, responsavelTecnico));
        dados.put("PAGINA_PROJETO", "");
        dados.put("PAGINA_AVISOS", "");
        dados.put("PAGINA_ITENS", paginaItens(orcamento));
        dados.put("PAGINA_TOTAIS", paginaTotais(orcamento, opcoes, observacoes, responsavelTecnico));

        return renderizar("orcamento.html", dados);
    }

    public byte[] gerarPdf(Long orcamentoId, GerarOrcamentoRequest request) {
        String html = gerarHtml(orcamentoId, request);
        try {
            return gerarPdfComNavegador(html);
        } catch (Exception ignored) {
            throw new IllegalStateException("Nao foi possivel gerar o PDF igual a pre-visualizacao HTML. Verifique se Chrome ou Edge esta instalado no servidor.", ignored);
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
        comando.add("--disable-gpu");
        comando.add("--disable-dev-shm-usage");
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
        List<String> candidatos = List.of(
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
        if (opcoes.getOrcamentoObjetivo() == null) {
            opcoes.setOrcamentoObjetivo(false);
        }
        if (opcoes.getImprimirTotal() == null) {
            opcoes.setImprimirTotal(false);
        }
        return opcoes;
    }

    private List<ObservacaoOrcamento> resolverObservacoes(Orcamento orcamento, GerarOrcamentoRequest request) {
        if (request != null
                && request.getObservacoesSelecionadas() != null) {
            return observacaoService.buscarSelecionadas(request.getObservacoesSelecionadas());
        }
        if (orcamento.getObservacoes() == null) {
            return List.of();
        }
        return orcamento.getObservacoes();
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

    private String paginaIdentificacao(Orcamento orcamento, OpcoesGeracaoRequest opcoes, String responsavelTecnico) {
        Cliente cliente = orcamento.getCliente();
        LocalDate vencimento = dataVencimento(orcamento);

        Map<String, String> dados = contextoBase();
        dados.put("CABECALHO_DOCUMENTO", cabecalho(orcamento));
        dados.put("NUMERO_ORCAMENTO", esc(numeroOrcamento(orcamento)));
        dados.put("NOME_CLIENTE", esc(nomeCliente(orcamento)));
        dados.put("CPF_CNPJ", esc(valor(cliente == null ? null : cliente.getCpfCnpj())));
        dados.put("TELEFONE_CLIENTE", esc(valor(cliente == null ? null : cliente.getTelefone())));
        dados.put("EMAIL_CLIENTE", esc(valor(cliente == null ? null : cliente.getEmail())));
        dados.put("ENDERECO_CLIENTE", esc(valor(cliente == null ? null : cliente.getEndereco())));
        dados.put("CIDADE_CLIENTE", esc(cidadeCliente(cliente)));
        dados.put("DATA_EMISSAO", esc(formatarData(orcamento.getDataEmissao())));
        dados.put("VALIDADE_DIAS", esc(validadeTexto(orcamento)));
        dados.put("DATA_VENCIMENTO", esc(formatarData(vencimento)));
        dados.put("SECAO_PROJETO", opcoes.isOrcamentoObjetivoAtivo() ? "" : paginaProjeto(orcamento, responsavelTecnico));
        dados.put("RODAPE", rodape());

        return renderizar("identificacao.html", dados);
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
                html.append("<figcaption>").append(esc(valor(imagem.getTitulo()))).append("</figcaption>");
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

    private String paginaAvisos(Orcamento orcamento, List<ObservacaoOrcamento> observacoes) {
        Map<String, String> dados = contextoBase();
        dados.put("PRAZO_EXECUCAO_DIAS", esc(valor(orcamento.getPrazoExecucaoDias())));
        dados.put("OBSERVACOES_SELECIONADAS", observacoesHtml(observacoes));

        return renderizar("avisos.html", dados);
    }

    private String paginaItens(Orcamento orcamento) {
        StringBuilder itensHtml = new StringBuilder();
        List<ItemOrcamento> itens = orcamento.getItemOrcamentoList() == null ? List.of() : orcamento.getItemOrcamentoList();

        for (ItemOrcamento item : itens) {
            Map<String, String> dados = contextoBase();
            dados.put("CODIGO_ITEM", esc(codigoItem(item)));
            dados.put("IMAGEM_ITEM", imagemOuPlaceholder(imagemItem(item), "product-thumb", "Imagem do item", "Sem imagem"));
            dados.put("NOME_ITEM", esc(nomeItem(item)));
            dados.put("DESCRICAO_ITEM", esc(descricaoItem(item)));
            dados.put("UNIDADE_ITEM", esc(unidadeItem(item)));
            dados.put("TIPO_PEDRA", esc(valor(item.getTipoPedra())));
            dados.put("ACABAMENTO", esc(valor(item.getAcabamento())));
            dados.put("DIMENSOES", esc(valor(item.getDimensoes())));
            dados.put("METROS_QUADRADOS", esc(valor(item.getMetrosQuadrados())));
            dados.put("QUANTIDADE", esc(valor(item.getQuantidade())));
            dados.put("PRECO_UNITARIO", esc(formatarMoeda(item.getPrecoUnitario())));
            dados.put("SUBTOTAL_ITEM", esc(formatarMoeda(item.getSubtotal())));
            dados.put("FRETE_CLASSE", Boolean.FALSE.equals(item.getFreteIncluso()) ? "no" : "yes");
            dados.put("FRETE_TEXTO", Boolean.FALSE.equals(item.getFreteIncluso()) ? "Frete não incluso" : "Frete incluso");
            itensHtml.append(renderizar("item.html", dados));
        }

        Financeiro financeiro = financeiroOuVazio(orcamento);
        Map<String, String> dados = contextoBase();
        dados.put("CABECALHO_DOCUMENTO", cabecalho(orcamento));
        dados.put("ITENS_ORCAMENTO", itensHtml.isEmpty() ? linhaTabelaVazia("Nenhum item cadastrado neste orçamento.") : itensHtml.toString());
        dados.put("DESCONTO_VALOR", esc(formatarMoeda(financeiro.getDescontoValorReais())));
        dados.put("TOTAL_FINAL", esc(formatarMoeda(financeiro.getTotalFinal())));
        dados.put("RODAPE", rodape());

        return renderizar("itens.html", dados);
    }

    private String paginaTotais(Orcamento orcamento, OpcoesGeracaoRequest opcoes, List<ObservacaoOrcamento> observacoes, String responsavelTecnico) {
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
            linhas.append(linhaTotal("Total geral", formatarMoeda(financeiro.getSubtotalItens()), true));
        }
        linhas.append(linhaTotal("Total final", formatarMoeda(financeiro.getTotalFinal()), true));

        Map<String, String> dados = contextoBase();
        dados.put("CABECALHO_DOCUMENTO", cabecalho(orcamento));
        dados.put("LINHAS_TOTAIS", linhas.toString());
        dados.put("VALOR_A_VISTA", esc(formatarMoeda(financeiro.getValorAVista())));
        dados.put("VALOR_ENTRADA", esc(formatarMoeda(financeiro.getEntrada50pct())));
        dados.put("VALOR_RESTANTE", esc(formatarMoeda(financeiro.getRestante50pct())));
        dados.put("DESCRICAO_RESTANTE", esc(valor(financeiro.getDescricaoRestante())));
        dados.put("PRAZO_PRODUCAO", esc(prazoProducao(orcamento)));
        dados.put("NOME_CLIENTE", esc(nomeCliente(orcamento)));
        dados.put("RESPONSAVEL_TECNICO", esc(responsavelTecnico));
        dados.put("SECAO_AVISOS", paginaAvisos(orcamento, observacoes));
        dados.put("RODAPE", rodape());

        return renderizar("totais.html", dados);
    }

    private String observacoesHtml(List<ObservacaoOrcamento> observacoes) {
        if (observacoes == null || observacoes.isEmpty()) {
            return "<li class=\"observation-item\">Nenhuma observação adicional selecionada.</li>";
        }

        StringBuilder itens = new StringBuilder();
        for (ObservacaoOrcamento observacao : observacoes) {
            Map<String, String> dadosItem = contextoBase();
            dadosItem.put("TEXTO_OBSERVACAO", esc(observacao.getTexto()));
            itens.append(renderizar("observacao-item.html", dadosItem));
        }

        Map<String, String> dados = contextoBase();
        dados.put("ITENS_OBSERVACOES", itens.toString());
        return renderizar("observacoes.html", dados);
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
