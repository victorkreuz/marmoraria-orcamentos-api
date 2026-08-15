package com.marmoraria.orcamentos.dto;

import lombok.Data;

@Data
public class OpcoesGeracaoRequest {
    private Boolean imprimirCapa;
    private Boolean imprimirTotal;
    private Boolean imprimirProjeto;
    private Boolean imprimirProjetoImagens;

    public boolean isImprimirCapaAtivo() {
        return Boolean.TRUE.equals(imprimirCapa);
    }

    public boolean isImprimirTotalAtivo() {
        return Boolean.TRUE.equals(imprimirTotal);
    }

    public boolean isImprimirProjetoAtivo() {
        return !Boolean.FALSE.equals(imprimirProjeto); // default true se não informado
    }

    public boolean isImprimirProjetoImagensAtivo() {
        return !Boolean.FALSE.equals(imprimirProjetoImagens); // default true se não informado
    }

    public static OpcoesGeracaoRequest padrao() {
        OpcoesGeracaoRequest opcoes = new OpcoesGeracaoRequest();
        opcoes.setImprimirCapa(false);
        opcoes.setImprimirTotal(true);
        opcoes.setImprimirProjeto(true);
        opcoes.setImprimirProjetoImagens(true);
        return opcoes;
    }
}
