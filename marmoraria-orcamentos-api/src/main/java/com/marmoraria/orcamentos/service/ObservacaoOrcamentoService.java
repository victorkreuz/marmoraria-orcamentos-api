package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.ObservacaoOrcamento;
import com.marmoraria.orcamentos.exception.ResourceNotFoundException;
import com.marmoraria.orcamentos.repository.ObservacaoOrcamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ObservacaoOrcamentoService {

    @Autowired
    private ObservacaoOrcamentoRepository observacaoRepository;

    public List<ObservacaoOrcamento> listarAtivas() {
        return observacaoRepository.findByAtivoTrueOrderByIdAsc();
    }

    public ObservacaoOrcamento buscarPorId(Long id) {
        return observacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Observacao nao encontrada"));
    }

    public List<ObservacaoOrcamento> buscarSelecionadas(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > 1) {
            throw new IllegalArgumentException("Selecione no maximo 1 observacao");
        }
        List<ObservacaoOrcamento> observacoes = observacaoRepository.findAllById(ids);
        if (observacoes.size() != ids.size()) {
            throw new ResourceNotFoundException("Uma ou mais observacoes nao foram encontradas");
        }
        return observacoes;
    }

    public ObservacaoOrcamento salvar(ObservacaoOrcamento observacao) {
        if (observacao.getAtivo() == null) {
            observacao.setAtivo(true);
        }
        preencherTitulo(observacao);
        return observacaoRepository.save(observacao);
    }

    public ObservacaoOrcamento editar(Long id, ObservacaoOrcamento observacao) {
        ObservacaoOrcamento existente = buscarPorId(id);
        existente.setTitulo(observacao.getTitulo());
        existente.setTexto(observacao.getTexto());
        if (observacao.getAtivo() != null) {
            existente.setAtivo(observacao.getAtivo());
        }
        preencherTitulo(existente);
        return observacaoRepository.save(existente);
    }

    @Transactional
    public void excluir(Long id) {
        ObservacaoOrcamento observacao = buscarPorId(id);
        if (observacao.getOrcamentos() != null && !observacao.getOrcamentos().isEmpty()) {
            observacao.setAtivo(false);
            observacaoRepository.save(observacao);
            return;
        }
        observacaoRepository.deleteById(id);
    }

    private void preencherTitulo(ObservacaoOrcamento observacao) {
        if (observacao.getTitulo() != null && !observacao.getTitulo().isBlank()) {
            return;
        }
        String texto = observacao.getTexto() == null ? "Observacao" : observacao.getTexto().trim();
        observacao.setTitulo(texto.length() <= 60 ? texto : texto.substring(0, 57) + "...");
    }
}
