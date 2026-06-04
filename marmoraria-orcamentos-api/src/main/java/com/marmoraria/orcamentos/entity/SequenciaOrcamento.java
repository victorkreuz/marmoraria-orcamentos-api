package com.marmoraria.orcamentos.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SequenciaOrcamento {

    @Id
    private Integer ano;

    private Integer ultimoNumero;
}
