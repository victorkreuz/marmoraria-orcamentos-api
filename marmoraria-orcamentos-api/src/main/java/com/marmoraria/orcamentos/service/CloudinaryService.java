package com.marmoraria.orcamentos.service;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service

public class CloudinaryService {

    @Autowired
    Cloudinary cloudinary;
    public Map<String, Object> salvar(MultipartFile multipartFile) throws IOException {
        return cloudinary.uploader().upload(multipartFile.getBytes(), Map.of());
    }
}
