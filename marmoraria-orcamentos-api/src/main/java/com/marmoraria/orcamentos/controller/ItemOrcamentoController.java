package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.service.ItemOrcamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item_rcamento")

public class ItemOrcamentoController {
    @Autowired
    private ItemOrcamentoService itemOrcamentoService;

    @GetMapping("/{id}")
    public ItemOrcamento buscarItemOrcamentoPorId(@PathVariable Long id) {
        return itemOrcamentoService.buscarPorId(id).get();
    }

    @GetMapping
    public List<ItemOrcamento> buscarItemOrcamento() {
        return itemOrcamentoService.buscarItemOrcamento();
    }

    @PostMapping
    public ItemOrcamento salvarItemOrcamento(@RequestBody ItemOrcamento itemOrcamento) {
        return itemOrcamentoService.salvar(itemOrcamento);
    }

    @PutMapping("/{id}")
    public ItemOrcamento editarItemOrcamento(@PathVariable Long id, @RequestBody ItemOrcamento itemOrcamento) {
        itemOrcamento.setId(id);
        return itemOrcamentoService.editar(itemOrcamento);
    }

    @DeleteMapping("/{id}")
    public String excluirItemOrcamento(@PathVariable Long id){
        itemOrcamentoService.excluir(id);
        return "Item excluído com sucesso!";
    }
}
