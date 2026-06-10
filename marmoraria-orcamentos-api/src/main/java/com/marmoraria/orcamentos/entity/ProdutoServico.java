package com.marmoraria.orcamentos.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProdutoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome e obrigatorio")
    private String nome;

    @NotNull(message = "Unidade de medida e obrigatoria")
    @Enumerated(EnumType.STRING)
    private UnidadeMedida unidadeMedida;

    @NotNull(message = "Valor unitario e obrigatorio")
    @DecimalMin(value = "0.00", message = "Valor unitario nao pode ser negativo")
    private BigDecimal valorUnitario;

    private String imagemPath;

    private String imagemUrl;
}
