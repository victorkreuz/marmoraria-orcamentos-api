package com.marmoraria.orcamentos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenBlocklistService tokenBlocklistService;

    public String autenticarUsuario(String username, String senha) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, senha));
        return jwtService.gerarToken(username);
    }

    public void logout(String token) {
        String jti = jwtService.extrairJti(token);
        tokenBlocklistService.revogar(jti, jwtService.extrairExpiracao(token));
    }
}
