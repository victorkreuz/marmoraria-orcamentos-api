package com.marmoraria.orcamentos.repository;

import com.marmoraria.orcamentos.entity.SequenciaOrcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SequenciaOrcamentoRepository extends JpaRepository<SequenciaOrcamento, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequencia from SequenciaOrcamento sequencia where sequencia.ano = :ano")
    Optional<SequenciaOrcamento> findByAnoForUpdate(@Param("ano") Integer ano);
}
