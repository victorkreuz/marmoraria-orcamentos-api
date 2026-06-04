package com.marmoraria.orcamentos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemOrcamentoImagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "URL da imagem e obrigatoria")
    private String url;

    private String publicId;

    private String nomeOriginal;

    private Integer ordem;

    @JsonBackReference("item-imagens")
    @ManyToOne
    @JoinColumn(name = "item_orcamento_id")
    private ItemOrcamento itemOrcamento;
}
