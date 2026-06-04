package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.service.OrcamentoService;
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
@RequestMapping("/api/orcamento")
public class OrcamentoController {
    @Autowired
    private OrcamentoService orcamentoService;

    @GetMapping("/{id}")
    public Orcamento buscarOrcamentoPorId(@PathVariable Long id) {
        return orcamentoService.buscarPorId(id);
    }

    @GetMapping
    public List<Orcamento> buscarOrcamento() {
        return orcamentoService.buscarOrcamento();
    }

    @PostMapping
    public Orcamento salvarOrcamento(@Valid @RequestBody Orcamento orcamento) {
        return orcamentoService.salvar(orcamento);
    }

    @PutMapping("/{id}")
    public Orcamento editarOrcamento(@PathVariable Long id, @Valid @RequestBody Orcamento orcamento) {
        orcamento.setId(id);
        return orcamentoService.editar(orcamento);
    }

    @DeleteMapping("/{id}")
    public String excluirOrcamento(@PathVariable Long id) {
        orcamentoService.excluir(id);
        return "Orcamento excluido com sucesso!";
    }
}
