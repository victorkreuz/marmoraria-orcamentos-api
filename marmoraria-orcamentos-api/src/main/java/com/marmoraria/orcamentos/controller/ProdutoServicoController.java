package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.entity.ProdutoServico;
import com.marmoraria.orcamentos.service.ProdutoServicoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/produto_servico")
public class ProdutoServicoController {
    @Autowired
    private ProdutoServicoService produtoServicoService;

    @GetMapping("/{id}")
    public ProdutoServico buscarProdutoServicoPorId(@PathVariable Long id) {
        return produtoServicoService.buscarPorId(id);
    }

    @GetMapping
    public List<ProdutoServico> buscarProdutoServico() {
        return produtoServicoService.buscarProdutoServico();
    }

    @PostMapping
    public ProdutoServico salvarProdutoServico(@Valid @RequestBody ProdutoServico produtoServico) {
        return produtoServicoService.salvar(produtoServico);
    }

    @PutMapping("/{id}")
    public ProdutoServico editarProdutoServico(@PathVariable Long id, @Valid @RequestBody ProdutoServico produtoServico) {
        produtoServico.setId(id);
        return produtoServicoService.editar(produtoServico);
    }

    @DeleteMapping("/{id}")
    public String excluirProdutoServico(@PathVariable Long id) {
        produtoServicoService.excluir(id);
        return "Produto/servico excluido com sucesso!";
    }
}
