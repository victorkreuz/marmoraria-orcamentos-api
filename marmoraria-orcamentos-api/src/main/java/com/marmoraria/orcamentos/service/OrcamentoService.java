package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.exception.ResourceNotFoundException;
import com.marmoraria.orcamentos.repository.ItemOrcamentoRepository;
import com.marmoraria.orcamentos.repository.OrcamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrcamentoService {
    @Autowired
    OrcamentoRepository orcamentoRepository;

    @Autowired
    ItemOrcamentoRepository itemOrcamentoRepository;

    @Autowired
    ItemOrcamentoService itemOrcamentoService;

    public Orcamento salvar(Orcamento orcamento) {
        calcularValorTotal(orcamento);
        return orcamentoRepository.save(orcamento);
    }

    public Orcamento buscarPorId(Long id) {
        return orcamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orcamento nao encontrado"));
    }

    public List<Orcamento> buscarOrcamento() {
        return orcamentoRepository.findAll();
    }

    public Orcamento editar(Orcamento orcamento) {
        buscarPorId(orcamento.getId());
        calcularValorTotal(orcamento);
        return orcamentoRepository.save(orcamento);
    }

    public void excluir(Long id) {
        buscarPorId(id);
        orcamentoRepository.deleteById(id);
    }

    public void calcularValorTotal(Orcamento orcamento) {
        BigDecimal totalItens = BigDecimal.ZERO;
        List<ItemOrcamento> itens = orcamento.getItemOrcamentoList();

        if ((itens == null || itens.isEmpty()) && orcamento.getId() != null) {
            itens = itemOrcamentoRepository.findByOrcamentoId(orcamento.getId());
        }

        if (itens != null) {
            for (ItemOrcamento item : itens) {
                item.setOrcamento(orcamento);
                itemOrcamentoService.calcularValorTotal(item);
                totalItens = totalItens.add(item.getValorTotal());
            }
        }

        BigDecimal valorDesconto = valorOuZero(orcamento.getValorDesconto());
        BigDecimal valorFrete = valorOuZero(orcamento.getValorFrete());
        BigDecimal valorTotal = totalItens.add(valorFrete).subtract(valorDesconto);

        if (valorTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor total do orcamento nao pode ser negativo");
        }

        orcamento.setValorTotal(valorTotal);
    }

    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
