package com.marmoraria.orcamentos.repository;

import com.marmoraria.orcamentos.entity.ObservacaoOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservacaoOrcamentoRepository extends JpaRepository<ObservacaoOrcamento, Long> {
    List<ObservacaoOrcamento> findByAtivoTrueOrderByIdAsc();
    boolean existsByOrcamentosId(Long orcamentoId);
}
