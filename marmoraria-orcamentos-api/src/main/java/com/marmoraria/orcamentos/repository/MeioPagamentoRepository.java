package com.marmoraria.orcamentos.repository;

import com.marmoraria.orcamentos.entity.MeioPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeioPagamentoRepository extends JpaRepository<MeioPagamento, Long> {
    List<MeioPagamento> findByAtivoTrueOrderByIdAsc();
}
