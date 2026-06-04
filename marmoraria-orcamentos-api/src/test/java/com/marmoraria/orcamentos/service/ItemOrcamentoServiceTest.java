package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.repository.ItemOrcamentoRepository;
import com.marmoraria.orcamentos.repository.OrcamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ItemOrcamentoServiceTest {

    @Mock
    private ItemOrcamentoRepository itemOrcamentoRepository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @InjectMocks
    private ItemOrcamentoService service;

    private ItemOrcamento item;

    @BeforeEach
    void setUp() {
        item = new ItemOrcamento();
        item.setQuantidade(1);
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setFreteIncluso(true);
    }

    @Test
    void calcularValorTotal_semDesconto_subtotalEhPrecoVezesQuantidade() {
        item.setQuantidade(3);
        item.setPrecoUnitario(new BigDecimal("50.00"));

        service.calcularValorTotal(item);

        assertThat(item.getSubtotal()).isEqualByComparingTo("150.00");
        assertThat(item.getValorTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void calcularValorTotal_comDesconto_subtotalEhPrecoMenosDesconto() {
        item.setPrecoUnitario(new BigDecimal("200.00"));
        item.setValorDesconto(new BigDecimal("50.00"));

        service.calcularValorTotal(item);

        assertThat(item.getSubtotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void calcularValorTotal_descontoMaiorQuePreco_lancaExcecao() {
        item.setPrecoUnitario(new BigDecimal("100.00"));
        item.setValorDesconto(new BigDecimal("150.00"));

        assertThatThrownBy(() -> service.calcularValorTotal(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao pode ser negativo");
    }

    @Test
    void calcularValorTotal_freteNaoIncluso_semFreteValor_lancaExcecao() {
        item.setFreteIncluso(false);
        item.setFreteValor(null);

        assertThatThrownBy(() -> service.calcularValorTotal(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Frete valor e obrigatorio");
    }

    @Test
    void calcularValorTotal_freteNaoIncluso_comFreteValor_calculaNormalmente() {
        item.setFreteIncluso(false);
        item.setFreteValor(new BigDecimal("25.00"));

        service.calcularValorTotal(item);

        assertThat(item.getSubtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void calcularValorTotal_usaValorUnitarioComoFallback_quandoPrecoUnitarioNulo() {
        item.setPrecoUnitario(null);
        item.setValorUnitario(new BigDecimal("80.00"));

        service.calcularValorTotal(item);

        assertThat(item.getPrecoUnitario()).isEqualByComparingTo("80.00");
        assertThat(item.getSubtotal()).isEqualByComparingTo("80.00");
    }

    @Test
    void calcularValorTotal_precoUnitarioNuloEValorUnitarioNulo_lancaExcecao() {
        item.setPrecoUnitario(null);
        item.setValorUnitario(null);

        assertThatThrownBy(() -> service.calcularValorTotal(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Preco unitario e obrigatorio");
    }

    @Test
    void calcularValorTotal_freteNuloSetaIncluso() {
        item.setFreteIncluso(null);

        service.calcularValorTotal(item);

        assertThat(item.getFreteIncluso()).isTrue();
    }

    @Test
    void calcularValorTotal_arredondamentoHalfUp() {
        item.setPrecoUnitario(new BigDecimal("33.333"));
        item.setQuantidade(3);

        service.calcularValorTotal(item);

        // 33.333 * 3 = 99.999 → rounds to 100.00 with HALF_UP
        assertThat(item.getSubtotal()).isEqualByComparingTo("100.00");
    }
}
