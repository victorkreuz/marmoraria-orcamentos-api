package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.entity.Cliente;
import com.marmoraria.orcamentos.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cliente")

public class ClienteController {
    @Autowired
    private ClienteService clienteService;

    @GetMapping("/{id}")
    public Cliente buscarClientePorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id).get();
    }

    @GetMapping
    public List<Cliente> buscarClientes() {
        return clienteService.buscarClientes();
    }

    @PostMapping
    public Cliente salvarCliente(@RequestBody Cliente cliente) {
        return clienteService.salvar(cliente);
    }

    @PutMapping("/{id}")
    public Cliente editarCliente(@PathVariable Long id, @RequestBody Cliente cliente) {
        cliente.setId(id);
        return clienteService.editar(cliente);
    }

    @DeleteMapping("/{id}")
    public String excluirCliente(@PathVariable Long id){
    clienteService.excluir(id);
    return "Cliente excluído com sucesso!";
    }
}
