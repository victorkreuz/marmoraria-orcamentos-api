package com.marmoraria.orcamentos.entity;

import jakarta.persistence.*;import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;import lombok.NoArgsConstructor;import java.math.BigDecimal;

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

    @ManyToOne
    @JoinColumn(name = "orcamento_id")
    private Orcamento orcamento;

    private Integer cod;
    private String descricao;
    private Integer quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal valorDesconto;
    private BigDecimal valorTotal;

}
