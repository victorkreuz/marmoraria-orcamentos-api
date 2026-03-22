package com.marmoraria.orcamentos.entity;

import jakarta.persistence.*;
import lombok.*;

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
    private String nome;
    private String cpfCnpj;
    private String email;
    private String telefone;
    private String endereco;
}