package com.marmoraria.orcamentos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtService jwtService;

    public String autenticarUsuario(String username, String senha) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, senha));
        return jwtService.gerarToken(username);
    }
}
