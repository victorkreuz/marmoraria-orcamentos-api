package com.marmoraria.orcamentos.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ObservacaoOrcamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    @NotBlank(message = "Texto da observacao e obrigatorio")
    @Column(columnDefinition = "TEXT")
    private String texto;

    private Boolean ativo;

    @JsonIgnore
    @ManyToMany(mappedBy = "observacoes")
    private List<Orcamento> orcamentos;
}
