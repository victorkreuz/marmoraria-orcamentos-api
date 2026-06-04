package com.marmoraria.orcamentos.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class ProdutoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome e obrigatorio")
    private String nome;

    @NotNull(message = "Unidade de medida e obrigatoria")
    private UnidadeMedida unidadeMedida;

    @NotNull(message = "Valor unitario e obrigatorio")
    @DecimalMin(value = "0.01", message = "Valor unitario deve ser maior que zero")
    private BigDecimal valorUnitario;

    private String imagemPath;
}
