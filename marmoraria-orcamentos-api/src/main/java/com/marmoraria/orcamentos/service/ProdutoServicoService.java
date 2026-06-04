package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.ProdutoServico;
import com.marmoraria.orcamentos.exception.ResourceNotFoundException;
import com.marmoraria.orcamentos.repository.ProdutoServicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoServicoService {
    @Autowired
    ProdutoServicoRepository produtoServicoRepository;

    public ProdutoServico salvar(ProdutoServico produtoServico) {
        return produtoServicoRepository.save(produtoServico);
    }

    public ProdutoServico buscarPorId(Long id) {
        return produtoServicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto/servico nao encontrado"));
    }

    public List<ProdutoServico> buscarProdutoServico() {
        return produtoServicoRepository.findAll();
    }

    public ProdutoServico editar(ProdutoServico produtoServico) {
        buscarPorId(produtoServico.getId());
        return produtoServicoRepository.save(produtoServico);
    }

    public void excluir(Long id) {
        buscarPorId(id);
        produtoServicoRepository.deleteById(id);
    }
}
