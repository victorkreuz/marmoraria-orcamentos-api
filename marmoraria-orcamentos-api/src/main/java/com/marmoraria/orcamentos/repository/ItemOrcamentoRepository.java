package com.marmoraria.orcamentos.repository;

import com.marmoraria.orcamentos.entity.ItemOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {
}
