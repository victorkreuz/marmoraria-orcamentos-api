package com.marmoraria.orcamentos.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {

    private static final int MAX_TENTATIVAS = 5;
    private static final long BLOQUEIO_MINUTOS = 15;

    private final ConcurrentHashMap<String, AtomicInteger> tentativas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> bloqueados = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String username) {
        Instant expiraEm = bloqueados.get(username);
        if (expiraEm == null) {
            return false;
        }
        if (Instant.now().isAfter(expiraEm)) {
            bloqueados.remove(username);
            tentativas.remove(username);
            return false;
        }
        return true;
    }

    public void registrarFalha(String username) {
        int total = tentativas
                .computeIfAbsent(username, k -> new AtomicInteger(0))
                .incrementAndGet();

        if (total >= MAX_TENTATIVAS) {
            bloqueados.put(username, Instant.now().plusSeconds(BLOQUEIO_MINUTOS * 60));
        }
    }

    public void registrarSucesso(String username) {
        tentativas.remove(username);
        bloqueados.remove(username);
    }
}