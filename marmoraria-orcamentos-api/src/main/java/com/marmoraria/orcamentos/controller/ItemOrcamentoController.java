package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.service.ItemOrcamentoService;
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
@RequestMapping("/api/item_orcamento")
public class ItemOrcamentoController {
    @Autowired
    private ItemOrcamentoService itemOrcamentoService;

    @GetMapping("/{id}")
    public ItemOrcamento buscarItemOrcamentoPorId(@PathVariable Long id) {
        return itemOrcamentoService.buscarPorId(id);
    }

    @GetMapping
    public List<ItemOrcamento> buscarItemOrcamento() {
        return itemOrcamentoService.buscarItemOrcamento();
    }

    @PostMapping
    public ItemOrcamento salvarItemOrcamento(@Valid @RequestBody ItemOrcamento itemOrcamento) {
        return itemOrcamentoService.salvar(itemOrcamento);
    }

    @PutMapping("/{id}")
    public ItemOrcamento editarItemOrcamento(@PathVariable Long id, @Valid @RequestBody ItemOrcamento itemOrcamento) {
        itemOrcamento.setId(id);
        return itemOrcamentoService.editar(itemOrcamento);
    }

    @DeleteMapping("/{id}")
    public String excluirItemOrcamento(@PathVariable Long id) {
        itemOrcamentoService.excluir(id);
        return "Item excluido com sucesso!";
    }
}
