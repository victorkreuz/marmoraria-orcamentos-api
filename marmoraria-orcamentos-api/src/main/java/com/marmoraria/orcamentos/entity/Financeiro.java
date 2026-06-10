package com.marmoraria.orcamentos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Financeiro {

    @Column(name = "valor_a_vista")
    private BigDecimal valorAVista;

    @DecimalMin(value = "0.00", message = "Subtotal dos itens nao pode ser negativo")
    private BigDecimal subtotalItens;

    @DecimalMin(value = "0.00", message = "Frete extra nao pode ser negativo")
    private BigDecimal freteExtra;

    private Boolean freteIncluso;

    @DecimalMin(value = "0.00", message = "Percentual de desconto nao pode ser negativo")
    private BigDecimal descontoPercentual;

    @DecimalMin(value = "0.00", message = "Valor de desconto nao pode ser negativo")
    private BigDecimal descontoValorReais;

    @DecimalMin(value = "0.00", message = "Adendos nao podem ser negativos")
    private BigDecimal adendos;

    private BigDecimal totalFinal;
    private BigDecimal entrada50pct;
    private BigDecimal restante50pct;
    private String descricaoRestante;
    private Long meioPagamentoId;
    private String meioPagamentoTitulo;
    private String meioPagamentoDescricao;
}
