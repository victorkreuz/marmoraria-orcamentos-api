package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.MeioPagamento;
import com.marmoraria.orcamentos.exception.ResourceNotFoundException;
import com.marmoraria.orcamentos.repository.MeioPagamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MeioPagamentoService {

    @Autowired
    private MeioPagamentoRepository meioPagamentoRepository;

    public List<MeioPagamento> listarAtivos() {
        return meioPagamentoRepository.findByAtivoTrueOrderByIdAsc();
    }

    public MeioPagamento buscarPorId(Long id) {
        return meioPagamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meio de pagamento nao encontrado"));
    }

    public MeioPagamento salvar(MeioPagamento meioPagamento) {
        if (meioPagamento.getAtivo() == null) {
            meioPagamento.setAtivo(true);
        }
        return meioPagamentoRepository.save(meioPagamento);
    }

    public MeioPagamento editar(Long id, MeioPagamento meioPagamento) {
        MeioPagamento existente = buscarPorId(id);
        existente.setTitulo(meioPagamento.getTitulo());
        existente.setDescricao(meioPagamento.getDescricao());
        if (meioPagamento.getAtivo() != null) {
            existente.setAtivo(meioPagamento.getAtivo());
        }
        return meioPagamentoRepository.save(existente);
    }

    public void excluir(Long id) {
        MeioPagamento meioPagamento = buscarPorId(id);
        meioPagamento.setAtivo(false);
        meioPagamentoRepository.save(meioPagamento);
    }
}
