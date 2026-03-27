package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.service.OrcamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orcamento")

public class OrcamentoController {
    @Autowired
    private OrcamentoService orcamentoService;

    @GetMapping("/{id}")
    public Orcamento buscarOrcamentoPorId(@PathVariable Long id) {
        return orcamentoService.buscarPorId(id).get();
    }

    @GetMapping
    public List<Orcamento> buscarOrcamento() {
        return orcamentoService.buscarOrcamento();
    }

    @PostMapping
    public Orcamento salvarOrcamento(@RequestBody Orcamento orcamento) {
        return orcamentoService.salvar(orcamento);
    }

    @PutMapping("/{id}")
    public Orcamento editarOrcamento(@PathVariable Long id, @RequestBody Orcamento orcamento) {
        orcamento.setId(id);
        return orcamentoService.editar(orcamento);
    }

    @DeleteMapping("/{id}")
    public String excluirOrcamento(@PathVariable Long id){
        orcamentoService.excluir(id);
        return "Orçamento excluído com sucesso!";
    }
}
