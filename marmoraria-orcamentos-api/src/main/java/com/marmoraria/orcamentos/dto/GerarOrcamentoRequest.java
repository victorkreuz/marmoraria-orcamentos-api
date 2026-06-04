package com.marmoraria.orcamentos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GerarOrcamentoRequest {
    @Valid
    private OpcoesGeracaoRequest opcoes;

    @Size(max = 1, message = "Selecione no maximo 1 observacao")
    private List<Long> observacoesSelecionadas;
}
