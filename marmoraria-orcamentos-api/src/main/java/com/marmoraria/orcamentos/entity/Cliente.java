package com.marmoraria.orcamentos.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer cod;

    @NotBlank(message = "Nome e obrigatorio")
    private String nome;

    @Pattern(regexp = "^$|^(\\d{11}|\\d{14})$", message = "CPF/CNPJ deve conter 11 ou 14 numeros")
    private String cpfCnpj;

    @Email(message = "Email invalido")
    private String email;

    @Pattern(regexp = "^$|^[0-9()+\\-\\s]{8,20}$", message = "Telefone invalido")
    private String telefone;

    private String endereco;

    private String cidade;
}
