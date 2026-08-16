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
import java.math.RoundingMode;
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
        validarValidadeDias(orcamento.getValidadeDias());

        BigDecimal totalItens = BigDecimal.ZERO;
        List<ItemOrcamento> itens = orcamento.getItemOrcamentoList();

        if ((itens == null || itens.isEmpty()) && orcamento.getId() != null) {
            itens = itemOrcamentoRepository.findByOrcamentoId(orcamento.getId());
        }

        if (itens != null) {
            for (ItemOrcamento item : itens) {
                item.setOrcamento(orcamento);
                itemOrcamentoService.calcularValorTotal(item);
                totalItens = totalItens.add(valorOuZero(item.getSubtotal()));
            }
        }

        Financeiro financeiro = orcamento.getFinanceiro();
        if (financeiro == null) {
            financeiro = new Financeiro();
            orcamento.setFinanceiro(financeiro);
        }

        BigDecimal descontoPercentual = financeiro.getDescontoPercentual();
        BigDecimal descontoValorReais;
        if (descontoPercentual != null && descontoPercentual.compareTo(BigDecimal.ZERO) > 0) {
            descontoValorReais = totalItens.multiply(descontoPercentual)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            financeiro.setDescontoValorReais(descontoValorReais);
        } else {
            // Sem percentual informado: respeita um desconto em reais preenchido diretamente.
            descontoValorReais = valorOuZero(financeiro.getDescontoValorReais());
        }

        BigDecimal freteExtra = Boolean.FALSE.equals(financeiro.getFreteIncluso())
                ? valorOuZero(financeiro.getFreteExtra())
                : BigDecimal.ZERO;
        BigDecimal adendos = valorOuZero(financeiro.getAdendos());

        BigDecimal valorTotal = totalItens
                .subtract(descontoValorReais)
                .add(freteExtra)
                .add(adendos);

        if (valorTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor total do orcamento nao pode ser negativo");
        }

        orcamento.setValorTotal(valorTotal);

        if (orcamento.getDataEmissao() != null && orcamento.getValidadeDias() != null) {
            orcamento.setDataValidade(orcamento.getDataEmissao().plusDays(orcamento.getValidadeDias()));
        }

        financeiro.setSubtotalItens(totalItens);
        financeiro.setTotalFinal(valorTotal);

        BigDecimal entrada50pct = valorTotal.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        financeiro.setEntrada50pct(entrada50pct);
        financeiro.setRestante50pct(valorTotal.subtract(entrada50pct));

        preencherMeioPagamento(financeiro);
    }

    private void validarValidadeDias(Integer validadeDias) {
        if (validadeDias != null && validadeDias != 15 && validadeDias != 30) {
            throw new IllegalArgumentException("Validade deve ser 15 ou 30 dias");
        }
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
