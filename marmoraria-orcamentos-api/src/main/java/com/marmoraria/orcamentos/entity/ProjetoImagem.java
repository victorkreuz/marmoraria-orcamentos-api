package com.marmoraria.orcamentos.entity;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjetoImagem {

    @NotBlank(message = "URL da imagem do projeto e obrigatoria")
    private String url;

    private String titulo;

    private String publicId;

    private String nomeOriginal;

    private Integer ordem;
}
