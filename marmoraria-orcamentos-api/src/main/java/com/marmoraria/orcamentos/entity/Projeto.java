package com.marmoraria.orcamentos.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Projeto {
    private String nome;
    private String tipoPedraPrincipal;
    private String fotoPrincipalUrl;
    private String observacoes;
    private String responsavelTecnico;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "orcamento_projeto_imagem", joinColumns = @JoinColumn(name = "orcamento_id"))
    @OrderColumn(name = "ordem_lista")
    @Builder.Default
    private List<ProjetoImagem> imagens = new ArrayList<>();
}