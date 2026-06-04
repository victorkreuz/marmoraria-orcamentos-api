package com.marmoraria.orcamentos.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Cliente e obrigatorio")
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    private Integer cod;

    @NotNull(message = "Data de emissao e obrigatoria")
    private LocalDate dataEmissao;

    @FutureOrPresent(message = "Data de validade nao pode estar no passado")
    private LocalDate dataValidade;

    @Valid
    @JsonManagedReference
    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrcamento> itemOrcamentoList;

    private BigDecimal valorTotal;

    @DecimalMin(value = "0.00", message = "Valor de desconto nao pode ser negativo")
    private BigDecimal valorDesconto;

    @DecimalMin(value = "0.00", message = "Valor de frete nao pode ser negativo")
    private BigDecimal valorFrete;

    @NotNull(message = "Status do orcamento e obrigatorio")
    private StatusOrcamento statusOrcamento;
}
