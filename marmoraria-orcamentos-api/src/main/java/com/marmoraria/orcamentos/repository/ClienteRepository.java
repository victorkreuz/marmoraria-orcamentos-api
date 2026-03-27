package com.marmoraria.orcamentos.repository;

import com.marmoraria.orcamentos.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
