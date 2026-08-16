package com.marmoraria.orcamentos.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OrcamentoTest {

    private Orcamento orcamentoComDias(StatusOrcamento status, int diasDesdeEmissao, int validadeDias) {
        Orcamento orcamento = new Orcamento();
        orcamento.setStatusOrcamento(status);
        orcamento.setDataEmissao(LocalDate.now().minusDays(diasDesdeEmissao));
        orcamento.setValidadeDias(validadeDias);
        return orcamento;
    }

    @Test
    void solicitadoVencidoViraExpiradoNaExibicao() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.SOLICITADO, 20, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.EXPIRADO);
        assertThat(orcamento.getStatusOrcamento()).isEqualTo(StatusOrcamento.SOLICITADO);
    }

    @Test
    void solicitadoQueVenceHojeAindaNaoEstaExpirado() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.SOLICITADO, 15, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.SOLICITADO);
    }

    @Test
    void enviadoDentroDaValidadeContinuaEnviado() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.ENVIADO, 5, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.ENVIADO);
    }

    @Test
    void aprovadoVencidoNaoViraExpirado() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.APROVADO, 20, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.APROVADO);
    }

    @Test
    void rejeitadoVencidoNaoViraExpirado() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.REJEITADO, 20, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.REJEITADO);
    }

    @Test
    void rascunhoVencidoNaoViraExpirado() {
        Orcamento orcamento = orcamentoComDias(StatusOrcamento.RASCUNHO, 20, 15);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.RASCUNHO);
    }

    @Test
    void semDataEmissaoNaoQuebraOCalculo() {
        Orcamento orcamento = new Orcamento();
        orcamento.setStatusOrcamento(StatusOrcamento.SOLICITADO);

        assertThat(orcamento.getStatusExibicao()).isEqualTo(StatusOrcamento.SOLICITADO);
    }
}
