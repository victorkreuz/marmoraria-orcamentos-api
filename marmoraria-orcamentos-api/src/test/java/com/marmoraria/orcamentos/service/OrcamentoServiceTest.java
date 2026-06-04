package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.Financeiro;
import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.entity.StatusOrcamento;
import com.marmoraria.orcamentos.repository.ItemOrcamentoRepository;
import com.marmoraria.orcamentos.repository.MeioPagamentoRepository;
import com.marmoraria.orcamentos.repository.OrcamentoRepository;
import com.marmoraria.orcamentos.repository.SequenciaOrcamentoRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock private OrcamentoRepository orcamentoRepository;
    @Mock private ItemOrcamentoRepository itemOrcamentoRepository;
    @Mock private ItemOrcamentoService itemOrcamentoService;
    @Mock private MeioPagamentoRepository meioPagamentoRepository;
    @Mock private SequenciaOrcamentoRepository sequenciaOrcamentoRepository;

    @InjectMocks
    private OrcamentoService service;

    private Orcamento orcamento;

    @BeforeEach
    void setUp() {
        // Stub the void method so it doesn't interfere with subtotals we set.
        // Use lenient() because some tests throw before reaching itemOrcamentoService.
        lenient().doNothing().when(itemOrcamentoService).calcularValorTotal(any(ItemOrcamento.class));

        orcamento = new Orcamento();
        orcamento.setDataEmissao(LocalDate.now());
        orcamento.setStatusOrcamento(StatusOrcamento.SOLICITADO);
        orcamento.setFinanceiro(new Financeiro());
    }

    private ItemOrcamento itemCom(String preco, int qtd) {
        ItemOrcamento item = new ItemOrcamento();
        item.setPrecoUnitario(new BigDecimal(preco));
        item.setQuantidade(qtd);
        item.setSubtotal(new BigDecimal(preco).multiply(BigDecimal.valueOf(qtd)));
        item.setFreteIncluso(true);
        return item;
    }

    @Test
    void calcularValorTotal_umItem_totalEhSubtotalDoItem() {
        ItemOrcamento item = itemCom("500.00", 1);
        orcamento.setItemOrcamentoList(List.of(item));

        service.calcularValorTotal(orcamento);

        assertThat(orcamento.getValorTotal()).isEqualByComparingTo("500.00");
        assertThat(orcamento.getFinanceiro().getTotalFinal()).isEqualByComparingTo("500.00");
    }

    @Test
    void calcularValorTotal_doisItens_totalEhSomaDosSubtotais() {
        orcamento.setItemOrcamentoList(List.of(itemCom("300.00", 1), itemCom("200.00", 2)));

        service.calcularValorTotal(orcamento);

        assertThat(orcamento.getValorTotal()).isEqualByComparingTo("700.00");
    }

    @Test
    void calcularValorTotal_comDescontoPercentual_totalEhReduzido() {
        orcamento.setItemOrcamentoList(List.of(itemCom("1000.00", 1)));
        orcamento.getFinanceiro().setDescontoPercentual(new BigDecimal("10"));

        service.calcularValorTotal(orcamento);

        assertThat(orcamento.getValorTotal()).isEqualByComparingTo("900.00");
        assertThat(orcamento.getFinanceiro().getDescontoValorReais()).isEqualByComparingTo("100.00");
    }

    @Test
    void calcularValorTotal_comFreteExtra_totalIncluiFrete() {
        orcamento.setItemOrcamentoList(List.of(itemCom("500.00", 1)));
        orcamento.getFinanceiro().setFreteIncluso(false);
        orcamento.getFinanceiro().setFreteExtra(new BigDecimal("50.00"));

        service.calcularValorTotal(orcamento);

        assertThat(orcamento.getValorTotal()).isEqualByComparingTo("550.00");
    }

    @Test
    void calcularValorTotal_freteInclusoTrue_naoAdicionaFrete() {
        orcamento.setItemOrcamentoList(List.of(itemCom("500.00", 1)));
        orcamento.getFinanceiro().setFreteIncluso(true);
        orcamento.getFinanceiro().setFreteExtra(new BigDecimal("50.00"));

        service.calcularValorTotal(orcamento);

        assertThat(orcamento.getValorTotal()).isEqualByComparingTo("500.00");
    }

    @Test
    void calcularValorTotal_comAdendos_totalEhAcrescido() {
        orcamento.setItemOrcamentoList(List.of(itemCom("500.00", 1)));
        orcamento.getFinanceiro().setAdendos(new BigDecimal("30.00"));

        service.calcularValorTotal(orcamento);

        assertThat(orcamento.getValorTotal()).isEqualByComparingTo("530.00");
    }

    @Test
    void calcularValorTotal_descontoMaiorQueTotal_lancaExcecao() {
        orcamento.setItemOrcamentoList(List.of(itemCom("100.00", 1)));
        orcamento.getFinanceiro().setDescontoPercentual(new BigDecimal("101"));

        assertThatThrownBy(() -> service.calcularValorTotal(orcamento))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao pode ser negativo");
    }

    @Test
    void calcularValorTotal_entrada50pctEhMetadeDeTotalFinal() {
        orcamento.setItemOrcamentoList(List.of(itemCom("1000.00", 1)));

        service.calcularValorTotal(orcamento);

        assertThat(orcamento.getFinanceiro().getEntrada50pct()).isEqualByComparingTo("500.00");
        assertThat(orcamento.getFinanceiro().getRestante50pct()).isEqualByComparingTo("500.00");
    }

    @Test
    void calcularValorTotal_dataEmissaoComValidadeDias_preencheDataValidade() {
        orcamento.setDataEmissao(LocalDate.of(2026, 1, 1));
        orcamento.setValidadeDias(30);
        orcamento.setItemOrcamentoList(List.of(itemCom("100.00", 1)));

        service.calcularValorTotal(orcamento);

        assertThat(orcamento.getDataValidade()).isEqualTo(LocalDate.of(2026, 1, 31));
    }

    @Test
    void calcularValorTotal_validadeDiasInvalida_lancaExcecao() {
        orcamento.setValidadeDias(20);
        orcamento.setItemOrcamentoList(List.of(itemCom("100.00", 1)));

        assertThatThrownBy(() -> service.calcularValorTotal(orcamento))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Validade deve ser 15 ou 30 dias");
    }
}
