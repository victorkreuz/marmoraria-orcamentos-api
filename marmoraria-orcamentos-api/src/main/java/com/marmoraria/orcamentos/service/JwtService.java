package com.marmoraria.orcamentos.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${JWT_SECRET}")
    private String secret;

    @Autowired
    private TokenBlocklistService tokenBlocklistService;

    private static final long EXPIRATION_MS = 1000L * 60 * 60 * 24;

    @PostConstruct
    public void validarChave() {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET deve ter no minimo 32 caracteres. Tamanho atual: "
                + (secret == null ? 0 : secret.getBytes().length));
        }
    }

    public String gerarToken(String username) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .subject(username)
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey())
                .compact();
    }

    public String extrairUsername(String token) {
        return extrairClaims(token).getSubject();
    }

    public String extrairJti(String token) {
        return extrairClaims(token).getId();
    }

    public Date extrairExpiracao(String token) {
        return extrairClaims(token).getExpiration();
    }

    public boolean validarToken(String token, UserDetails userDetails) {
        String jti = extrairJti(token);
        if (tokenBlocklistService.estaRevogado(jti)) {
            return false;
        }
        String username = extrairUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpirado(token);
    }

    private boolean isTokenExpirado(String token) {
        return extrairClaims(token).getExpiration().before(new Date());
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
