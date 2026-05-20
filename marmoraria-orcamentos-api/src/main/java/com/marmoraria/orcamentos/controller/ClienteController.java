package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.entity.Cliente;
import com.marmoraria.orcamentos.service.ClienteService;
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
@RequestMapping("/api/cliente")
public class ClienteController {
    @Autowired
    private ClienteService clienteService;

    @GetMapping("/{id}")
    public Cliente buscarClientePorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @GetMapping
    public List<Cliente> buscarClientes() {
        return clienteService.buscarClientes();
    }

    @PostMapping
    public Cliente salvarCliente(@Valid @RequestBody Cliente cliente) {
        return clienteService.salvar(cliente);
    }

    @PutMapping("/{id}")
    public Cliente editarCliente(@PathVariable Long id, @Valid @RequestBody Cliente cliente) {
        cliente.setId(id);
        return clienteService.editar(cliente);
    }

    @DeleteMapping("/{id}")
    public String excluirCliente(@PathVariable Long id) {
        clienteService.excluir(id);
        return "Cliente excluido com sucesso!";
    }
}
