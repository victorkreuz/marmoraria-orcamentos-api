package com.marmoraria.orcamentos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.CascadeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

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

    private String nome;
    private String descricao;
    private String tipoPedra;
    private String acabamento;
    private String dimensoes;

    @DecimalMin(value = "0.00", message = "Metros quadrados nao pode ser negativo")
    private BigDecimal metrosQuadrados;

    @NotNull(message = "Quantidade e obrigatoria")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    @DecimalMin(value = "0.00", message = "Preco unitario nao pode ser negativo")
    private BigDecimal precoUnitario;

    private BigDecimal subtotal;

    private Boolean freteIncluso;

    @DecimalMin(value = "0.00", message = "Valor de frete nao pode ser negativo")
    private BigDecimal freteValor;

    private String imagemUrl;

    @com.fasterxml.jackson.annotation.JsonManagedReference("item-imagens")
    @OneToMany(mappedBy = "itemOrcamento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("ordem ASC, id ASC")
    private List<ItemOrcamentoImagem> imagens;

    @DecimalMin(value = "0.00", message = "Valor unitario nao pode ser negativo")
    private BigDecimal valorUnitario;

    @DecimalMin(value = "0.00", message = "Valor de desconto nao pode ser negativo")
    private BigDecimal valorDesconto;

    private BigDecimal valorTotal;

    private Integer ordem;
}
