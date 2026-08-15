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
