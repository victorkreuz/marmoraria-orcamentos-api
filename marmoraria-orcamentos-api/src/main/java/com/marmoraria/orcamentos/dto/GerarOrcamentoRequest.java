package com.marmoraria.orcamentos.dto;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class GerarOrcamentoRequest {
    @Valid
    private OpcoesGeracaoRequest opcoes;

    private String observacoesDocumento;
}
