package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.Cliente;
import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.entity.Projeto;
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
    void projetoComNomeEObservacoesMostraAmbosMasNaoOsOutrosCampos() {
        Projeto projeto = new Projeto();
        projeto.setNome("Cozinha Gourmet");
        projeto.setObservacoes("Bancada em L, cuba dupla.");
        orcamento.setProjeto(projeto);

        String html = service.gerarHtml(1L, null);

        assertThat(html).contains("Nome do Projeto");
        assertThat(html).contains("Cozinha Gourmet");
        assertThat(html).contains("Observações do Projeto");
        assertThat(html).contains("Bancada em L, cuba dupla.");
        assertThat(html).doesNotContain("Material / Ambiente");
        assertThat(html).doesNotContain("Responsável Técnico");
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
}
