package com.marmoraria.orcamentos.repository;

import com.marmoraria.orcamentos.entity.ItemOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {
    List<ItemOrcamento> findByOrcamentoId(Long orcamentoId);
}
