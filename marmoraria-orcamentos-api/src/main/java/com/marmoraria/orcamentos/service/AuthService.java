package com.marmoraria.orcamentos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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

    @Autowired
    private LoginAttemptService loginAttemptService;

    public String autenticarUsuario(String username, String senha) {
        if (loginAttemptService.estaBloqueado(username)) {
            throw new com.marmoraria.orcamentos.exception.ContaBloqueadaException(
                    "Muitas tentativas falhas. Tente novamente em alguns minutos.");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, senha));
        } catch (BadCredentialsException exception) {
            loginAttemptService.registrarFalha(username);
            throw exception;
        }

        loginAttemptService.registrarSucesso(username);
        return jwtService.gerarToken(username);
    }

    public void logout(String token) {
        String jti = jwtService.extrairJti(token);
        tokenBlocklistService.revogar(jti, jwtService.extrairExpiracao(token));
    }
}