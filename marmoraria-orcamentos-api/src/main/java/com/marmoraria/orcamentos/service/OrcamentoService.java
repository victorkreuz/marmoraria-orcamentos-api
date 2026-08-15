package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.Financeiro;
import com.marmoraria.orcamentos.entity.ItemOrcamento;
import com.marmoraria.orcamentos.entity.MeioPagamento;
import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.exception.ResourceNotFoundException;
import com.marmoraria.orcamentos.repository.ItemOrcamentoRepository;
import com.marmoraria.orcamentos.repository.MeioPagamentoRepository;
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

    @Autowired
    MeioPagamentoRepository meioPagamentoRepository;

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
        boolean itensFornecidosPeloChamador = itens != null && !itens.isEmpty();

        if (!itensFornecidosPeloChamador && orcamento.getId() != null) {
            itens = itemOrcamentoRepository.findByOrcamentoId(orcamento.getId());
        }

        if (itens != null) {
            for (int i = 0; i < itens.size(); i++) {
                ItemOrcamento item = itens.get(i);
                item.setOrcamento(orcamento);
                if (itensFornecidosPeloChamador) {
                    item.setOrdem(i);
                }
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

        Financeiro financeiro = orcamento.getFinanceiro();
        if (financeiro == null) {
            financeiro = new Financeiro();
            orcamento.setFinanceiro(financeiro);
        }
        financeiro.setSubtotalItens(totalItens);
        financeiro.setTotalFinal(valorTotal);
        preencherMeioPagamento(financeiro);
    }

    private void preencherMeioPagamento(Financeiro financeiro) {
        Long id = financeiro.getMeioPagamentoId();
        if (id == null) {
            financeiro.setMeioPagamentoTitulo(null);
            financeiro.setMeioPagamentoDescricao(null);
            return;
        }
        meioPagamentoRepository.findById(id).ifPresent(mp -> {
            financeiro.setMeioPagamentoTitulo(mp.getTitulo());
            financeiro.setMeioPagamentoDescricao(mp.getDescricao());
        });
    }

    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
