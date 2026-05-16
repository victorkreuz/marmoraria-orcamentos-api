package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.dto.LoginRequest;
import com.marmoraria.orcamentos.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.cloudinary.AccessControlRule.AccessType.token;

@RestController
@RequestMapping("/api/auth/login")

public class AuthController {

    @Autowired
    AuthService authService;

    @PostMapping
    public String autenticarUsuario(@RequestBody LoginRequest loginRequest) {
        return authService.autenticarUsuario(loginRequest.getUsername(), loginRequest.getSenha());
    }
}
