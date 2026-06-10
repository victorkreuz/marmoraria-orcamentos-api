package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.exception.ResourceNotFoundException;
import com.marmoraria.orcamentos.repository.ItemOrcamentoRepository;
import com.marmoraria.orcamentos.repository.OrcamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ItemOrcamentoService {
    @Autowired
    ItemOrcamentoRepository itemOrcamentoRepository;

    @Autowired
    OrcamentoRepository orcamentoRepository;

    @Transactional
    public ItemOrcamento salvar(ItemOrcamento itemOrcamento) {
        calcularValorTotal(itemOrcamento);
        ItemOrcamento itemSalvo = itemOrcamentoRepository.save(itemOrcamento);
        recalcularOrcamentoDoItem(itemSalvo);
        return itemSalvo;
    }

    public ItemOrcamento buscarPorId(Long id) {
        return itemOrcamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item nao encontrado"));
    }

    public List<ItemOrcamento> buscarItemOrcamento() {
        return itemOrcamentoRepository.findAll();
    }

    @Transactional
    public ItemOrcamento editar(ItemOrcamento itemOrcamento) {
        ItemOrcamento itemExistente = buscarPorId(itemOrcamento.getId());
        if (itemOrcamento.getOrcamento() == null) {
            itemOrcamento.setOrcamento(itemExistente.getOrcamento());
        }
        calcularValorTotal(itemOrcamento);
        ItemOrcamento itemSalvo = itemOrcamentoRepository.save(itemOrcamento);
        recalcularOrcamentoDoItem(itemSalvo);
        return itemSalvo;
    }

    @Transactional
    public void excluir(Long id) {
        ItemOrcamento item = buscarPorId(id);
        Long orcamentoId = item.getOrcamento() == null ? null : item.getOrcamento().getId();
        itemOrcamentoRepository.deleteById(id);
        recalcularOrcamento(orcamentoId);
    }

    public void calcularValorTotal(ItemOrcamento itemOrcamento) {
        BigDecimal valorUnitario = valorOuZero(itemOrcamento.getValorUnitario());
        BigDecimal valorDesconto = valorOuZero(itemOrcamento.getValorDesconto());
        BigDecimal quantidade = BigDecimal.valueOf(itemOrcamento.getQuantidade());
        BigDecimal valorTotal = valorUnitario.multiply(quantidade).subtract(valorDesconto);

        if (valorTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor total do item nao pode ser negativo");
        }

        itemOrcamento.setValorTotal(valorTotal);
    }

    private void recalcularOrcamentoDoItem(ItemOrcamento itemOrcamento) {
        Long orcamentoId = itemOrcamento.getOrcamento() == null ? null : itemOrcamento.getOrcamento().getId();
        recalcularOrcamento(orcamentoId);
    }

    private void recalcularOrcamento(Long orcamentoId) {
        if (orcamentoId == null) {
            return;
        }

        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Orcamento nao encontrado"));

        BigDecimal totalItens = itemOrcamentoRepository.findByOrcamentoId(orcamentoId).stream()
                .map(ItemOrcamento::getValorTotal)
                .map(this::valorOuZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorTotal = totalItens
                .add(valorOuZero(orcamento.getValorFrete()))
                .subtract(valorOuZero(orcamento.getValorDesconto()));

        if (valorTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor total do orcamento nao pode ser negativo");
        }

        orcamento.setValorTotal(valorTotal);
        orcamentoRepository.save(orcamento);
    }

    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
