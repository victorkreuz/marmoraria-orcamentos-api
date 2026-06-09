package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.dto.GerarOrcamentoRequest;
import com.marmoraria.orcamentos.entity.Orcamento;
import com.marmoraria.orcamentos.service.OrcamentoDocumentoService;
import com.marmoraria.orcamentos.service.OrcamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orcamento")
public class OrcamentoController {
    @Autowired
    private OrcamentoService orcamentoService;

    @Autowired
    private OrcamentoDocumentoService orcamentoDocumentoService;

    @GetMapping("/{id}")
    public Orcamento buscarOrcamentoPorId(@PathVariable Long id) {
        return orcamentoService.buscarPorId(id);
    }

    @GetMapping
    public List<Orcamento> buscarOrcamento() {
        return orcamentoService.buscarOrcamento();
    }

    @PostMapping
    public Orcamento salvarOrcamento(@Valid @RequestBody Orcamento orcamento) {
        return orcamentoService.salvar(orcamento);
    }

    @PutMapping("/{id}")
    public Orcamento editarOrcamento(@PathVariable Long id, @Valid @RequestBody Orcamento orcamento) {
        orcamento.setId(id);
        return orcamentoService.editar(orcamento);
    }

    @DeleteMapping("/{id}")
    public String excluirOrcamento(@PathVariable Long id) {
        orcamentoService.excluir(id);
        return "Orcamento excluido com sucesso!";
    }

    @PostMapping("/{id}/html")
    public ResponseEntity<String> gerarHtml(@PathVariable Long id, @RequestBody(required = false) GerarOrcamentoRequest request) {
        String html = orcamentoDocumentoService.gerarHtml(id, request);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @PostMapping("/{id}/gerar")
    public ResponseEntity<byte[]> gerarPdf(@PathVariable Long id, @RequestBody(required = false) GerarOrcamentoRequest request) {
        byte[] pdf = orcamentoDocumentoService.gerarPdf(id, request);
        String nomeArquivo = orcamentoDocumentoService.nomeArquivoPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArquivo + "\"")
                .body(pdf);
    }
}
