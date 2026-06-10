package com.marmoraria.orcamentos.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^$|^\\d{4}-\\d{4}$", message = "Numero do orcamento deve seguir o formato AAAA-NNNN")
    private String numero;

    @NotNull(message = "Cliente e obrigatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    private Integer cod;

    @NotNull(message = "Data de emissao e obrigatoria")
    private LocalDate dataEmissao;

    @Min(value = 15, message = "Validade deve ser 15 ou 30 dias")
    @Max(value = 30, message = "Validade deve ser 15 ou 30 dias")
    private Integer validadeDias;

    @Min(value = 1, message = "Prazo de execucao deve ser maior que zero")
    private Integer prazoExecucaoDias;

    @FutureOrPresent(message = "Data de validade nao pode estar no passado")
    private LocalDate dataValidade;

    @Valid
    @Embedded
    private Projeto projeto;

    @Valid
    @JsonManagedReference
    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemOrcamento> itemOrcamentoList;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "orcamento_observacao",
            joinColumns = @JoinColumn(name = "orcamento_id"),
            inverseJoinColumns = @JoinColumn(name = "observacao_id")
    )
    private List<ObservacaoOrcamento> observacoes;

    @Valid
    @Embedded
    private Financeiro financeiro;

    private BigDecimal valorTotal;

    @DecimalMin(value = "0.00", message = "Valor de desconto nao pode ser negativo")
    private BigDecimal valorDesconto;

    @DecimalMin(value = "0.00", message = "Valor de frete nao pode ser negativo")
    private BigDecimal valorFrete;

    @NotNull(message = "Status do orcamento e obrigatorio")
    @Enumerated(EnumType.STRING)
    private StatusOrcamento statusOrcamento;
}