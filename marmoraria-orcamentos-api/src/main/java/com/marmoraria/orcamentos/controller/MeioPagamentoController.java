package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.entity.MeioPagamento;
import com.marmoraria.orcamentos.service.MeioPagamentoService;
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
@RequestMapping("/api/meios_pagamento")
public class MeioPagamentoController {

    @Autowired
    private MeioPagamentoService meioPagamentoService;

    @GetMapping
    public List<MeioPagamento> listarAtivos() {
        return meioPagamentoService.listarAtivos();
    }

    @PostMapping
    public MeioPagamento salvar(@Valid @RequestBody MeioPagamento meioPagamento) {
        return meioPagamentoService.salvar(meioPagamento);
    }

    @PutMapping("/{id}")
    public MeioPagamento editar(@PathVariable Long id, @Valid @RequestBody MeioPagamento meioPagamento) {
        return meioPagamentoService.editar(id, meioPagamento);
    }

    @DeleteMapping("/{id}")
    public String excluir(@PathVariable Long id) {
        meioPagamentoService.excluir(id);
        return "Meio de pagamento excluido com sucesso!";
    }
}
