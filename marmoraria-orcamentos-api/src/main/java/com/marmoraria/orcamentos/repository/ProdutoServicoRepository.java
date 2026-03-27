package com.marmoraria.orcamentos.repository;

import com.marmoraria.orcamentos.entity.ProdutoServico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoServicoRepository extends JpaRepository<ProdutoServico, Long> {
}
