package com.marmoraria.orcamentos.entity;

import java.time.LocalDate;

public final class OrcamentoDatas {

    private OrcamentoDatas() {
    }

    public static LocalDate vencimento(Orcamento orcamento) {
        if (orcamento.getDataEmissao() != null && orcamento.getValidadeDias() != null) {
            return orcamento.getDataEmissao().plusDays(orcamento.getValidadeDias());
        }
        return orcamento.getDataValidade();
    }
}
