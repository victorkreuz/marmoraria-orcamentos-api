package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.entity.ProdutoServico;
import com.marmoraria.orcamentos.service.ProdutoServicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produto_servico")

public class ProdutoServicoController {
    @Autowired
    private ProdutoServicoService produtoServicoService;

    @GetMapping("/{id}")
    public ProdutoServico buscarProdutoServicoPorId(@PathVariable Long id) {
        return produtoServicoService.buscarPorId(id).get();
    }

    @GetMapping
    public List<ProdutoServico> buscarProdutoServico() {
        return produtoServicoService.buscarProdutoServico();
    }

    @PostMapping
    public ProdutoServico salvarProdutoServico(@RequestBody ProdutoServico produtoServico) {
        return produtoServicoService.salvar(produtoServico);
    }

    @PutMapping("/{id}")
    public ProdutoServico editarProdutoServico(@PathVariable Long id, @RequestBody ProdutoServico produtoServico) {
        produtoServico.setId(id);
        return produtoServicoService.editar(produtoServico);
    }

    @DeleteMapping("/{id}")
    public String excluirItemOrcamento(@PathVariable Long id){
        produtoServicoService.excluir(id);
        return "Item excluído com sucesso!";
    }
}
