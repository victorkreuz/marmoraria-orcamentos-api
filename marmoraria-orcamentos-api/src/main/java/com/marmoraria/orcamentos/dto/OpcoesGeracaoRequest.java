package com.marmoraria.orcamentos.dto;

import lombok.Data;

@Data
public class OpcoesGeracaoRequest {
    private Boolean imprimirCapa;
    private Boolean imprimirTotal;

    public boolean isImprimirCapaAtivo() {
        return Boolean.TRUE.equals(imprimirCapa);
    }

    public boolean isImprimirTotalAtivo() {
        return Boolean.TRUE.equals(imprimirTotal);
    }

    public static OpcoesGeracaoRequest padrao() {
        OpcoesGeracaoRequest opcoes = new OpcoesGeracaoRequest();
        opcoes.setImprimirCapa(false);
        opcoes.setImprimirTotal(true);
        return opcoes;
    }
}
