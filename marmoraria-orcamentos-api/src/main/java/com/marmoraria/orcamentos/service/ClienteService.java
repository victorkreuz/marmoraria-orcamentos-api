package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.Cliente;
import com.marmoraria.orcamentos.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service

public class ClienteService {

    @Autowired
    ClienteRepository clienteRepository;

    public Cliente salvar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    public List<Cliente> buscarClientes() {
        return clienteRepository.findAll();
    }

    public Cliente editar(Cliente cliente) {
        if (!buscarPorId(cliente.getId()).isPresent()) {
            throw new RuntimeException("Cliente não encontrado");
        }
        return clienteRepository.save(cliente);
    }

    public void excluir(Long id) {
        clienteRepository.deleteById(id);
    }
}
