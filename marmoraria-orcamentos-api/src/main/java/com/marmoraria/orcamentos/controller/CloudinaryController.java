package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload_imagem")

public class CloudinaryController {
    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping
    public Map<String, Object> uploadImagem(@RequestParam MultipartFile file) throws IOException {
        return cloudinaryService.salvar(file);
    }
}
