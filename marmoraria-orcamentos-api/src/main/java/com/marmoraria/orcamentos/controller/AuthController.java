package com.marmoraria.orcamentos.controller;

import com.marmoraria.orcamentos.dto.LoginRequest;
import com.marmoraria.orcamentos.dto.LoginResponse;
import com.marmoraria.orcamentos.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthService authService;

    @PostMapping("/login")
    public LoginResponse autenticarUsuario(@Valid @RequestBody LoginRequest loginRequest) {
        String token = authService.autenticarUsuario(loginRequest.getUsername(), loginRequest.getSenha());
        return new LoginResponse(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token de autenticacao ausente ou invalido");
        }
        authService.logout(authHeader.substring(7));
        return ResponseEntity.ok("Logout realizado com sucesso");
    }
}
