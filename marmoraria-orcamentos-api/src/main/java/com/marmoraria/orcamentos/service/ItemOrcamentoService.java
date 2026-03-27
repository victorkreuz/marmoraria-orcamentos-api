package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.repository.ItemOrcamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class ItemOrcamentoService {
    @Autowired
    ItemOrcamentoRepository itemOrcamentoRepository;

    public ItemOrcamento salvar(ItemOrcamento itemOrcamento) {
        return itemOrcamentoRepository.save(itemOrcamento);
    }

    public Optional<ItemOrcamento> buscarPorId(Long id) {
        return itemOrcamentoRepository.findById(id);
    }

    public List<ItemOrcamento> buscarItemOrcamento() {
        return itemOrcamentoRepository.findAll();
    }

    public ItemOrcamento editar(ItemOrcamento itemOrcamento) {
        if (!buscarPorId(itemOrcamento.getId()).isPresent()) {
            throw new RuntimeException("Item não encontrado");
        }
        return itemOrcamentoRepository.save(itemOrcamento);
    }

    public void excluir(Long id) {
        itemOrcamentoRepository.deleteById(id);
    }
}

