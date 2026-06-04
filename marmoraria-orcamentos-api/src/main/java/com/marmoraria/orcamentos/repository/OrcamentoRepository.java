package com.marmoraria.orcamentos.repository;

import com.marmoraria.orcamentos.entity.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {
}
