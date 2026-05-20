package com.marmoraria.orcamentos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemOrcamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produto_servico_id")
    private ProdutoServico produtoServico;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "orcamento_id")
    private Orcamento orcamento;

    private Integer cod;
    private String descricao;

    @NotNull(message = "Quantidade e obrigatoria")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    @NotNull(message = "Valor unitario e obrigatorio")
    @DecimalMin(value = "0.00", message = "Valor unitario nao pode ser negativo")
    private BigDecimal valorUnitario;

    @DecimalMin(value = "0.00", message = "Valor de desconto nao pode ser negativo")
    private BigDecimal valorDesconto;

    private BigDecimal valorTotal;
}
