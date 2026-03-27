package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.repository.ItemOrcamentoRepository;
import com.marmoraria.orcamentos.repository.OrcamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class OrcamentoService {
    @Autowired
    OrcamentoRepository orcamentoRepository;

    public Orcamento salvar(Orcamento orcamento) {
        return orcamentoRepository.save(orcamento);
    }

    public Optional<Orcamento> buscarPorId(Long id) {
        return orcamentoRepository.findById(id);
    }

    public List<Orcamento> buscarOrcamento() {
        return orcamentoRepository.findAll();
    }

    public Orcamento editar(Orcamento orcamento) {
        if (!buscarPorId(orcamento.getId()).isPresent()) {
            throw new RuntimeException("Orçamento não encontrado");
        }
        return orcamentoRepository.save(orcamento);
    }

    public void excluir(Long id) {
        orcamentoRepository.deleteById(id);
    }
}
