package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.ProdutoServico;
import com.marmoraria.orcamentos.repository.ProdutoServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class ProdutoServicoService {
    @Autowired
    ProdutoServicoRepository produtoServicoRepository;

    public ProdutoServico salvar(ProdutoServico produtoServico) {
        return produtoServicoRepository.save(produtoServico);
    }

    public Optional<ProdutoServico> buscarPorId(Long id) {
        return produtoServicoRepository.findById(id);
    }

    public List<ProdutoServico> buscarProdutoServico() {
        return produtoServicoRepository.findAll();
    }

    public ProdutoServico editar(ProdutoServico produtoServico) {
        if (!buscarPorId(produtoServico.getId()).isPresent()) {
            throw new RuntimeException("Orçamento não encontrado");
        }
        return produtoServicoRepository.save(produtoServico);
    }

    public void excluir(Long id) {
        produtoServicoRepository.deleteById(id);
    }
}
