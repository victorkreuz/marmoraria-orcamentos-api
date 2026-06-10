package com.marmoraria.orcamentos.dto;

import lombok.Data;

@Data
public class OpcoesGeracaoRequest {
    private Boolean imprimirCapa;
    private Boolean imprimirResumo;
    private Boolean orcamentoObjetivo;
    private Boolean imprimirTotal;
    private Boolean pularItens;

    public boolean isImprimirCapaAtivo() {
        return Boolean.TRUE.equals(imprimirCapa);
    }

    public boolean isImprimirResumoAtivo() {
        return Boolean.TRUE.equals(imprimirResumo);
    }

    public boolean isOrcamentoObjetivoAtivo() {
        return Boolean.TRUE.equals(orcamentoObjetivo);
    }

    public boolean isImprimirTotalAtivo() {
        return Boolean.TRUE.equals(imprimirTotal);
    }

    public boolean isPularItensAtivo() {
        return Boolean.TRUE.equals(pularItens);
    }

    public static OpcoesGeracaoRequest padrao() {
        OpcoesGeracaoRequest opcoes = new OpcoesGeracaoRequest();
        opcoes.setImprimirCapa(false);
        opcoes.setOrcamentoObjetivo(false);
        opcoes.setImprimirTotal(true);
        opcoes.setPularItens(false);
        return opcoes;
    }
}
