package com.marmoraria.orcamentos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    private Integer cod;

    private LocalDate dataEmissao;

    private LocalDate dataValidade;

    @OneToMany(mappedBy = "orcamento")
    private List<ItemOrcamento> itemOrcamentoList;

    private BigDecimal valorTotal;
    private BigDecimal valorDesconto;
    private BigDecimal valorFrete;

    private StatusOrcamento statusOrcamento;

}




