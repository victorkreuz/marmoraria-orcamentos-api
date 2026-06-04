package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.entity.ObservacaoOrcamento;
import com.marmoraria.orcamentos.service.ObservacaoOrcamentoService;
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
@RequestMapping("/api/observacoes")
public class ObservacaoOrcamentoController {

    @Autowired
    private ObservacaoOrcamentoService observacaoService;

    @GetMapping
    public List<ObservacaoOrcamento> listarAtivas() {
        return observacaoService.listarAtivas();
    }

    @PostMapping
    public ObservacaoOrcamento salvar(@Valid @RequestBody ObservacaoOrcamento observacao) {
        return observacaoService.salvar(observacao);
    }

    @PutMapping("/{id}")
    public ObservacaoOrcamento editar(@PathVariable Long id, @Valid @RequestBody ObservacaoOrcamento observacao) {
        return observacaoService.editar(id, observacao);
    }

    @DeleteMapping("/{id}")
    public String excluir(@PathVariable Long id) {
        observacaoService.excluir(id);
        return "Observacao excluida com sucesso!";
    }
}
