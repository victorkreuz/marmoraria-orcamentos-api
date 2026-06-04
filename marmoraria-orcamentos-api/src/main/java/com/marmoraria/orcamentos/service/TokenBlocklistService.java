package com.marmoraria.orcamentos.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlocklistService {

    private final Map<String, Long> blocklist = new ConcurrentHashMap<>();

    public void revogar(String jti, Date expiracao) {
        blocklist.put(jti, expiracao.getTime());
    }

    public boolean estaRevogado(String jti) {
        return blocklist.containsKey(jti);
    }

    @Scheduled(fixedRate = 3_600_000)
    public void limparExpirados() {
        long agora = System.currentTimeMillis();
        blocklist.entrySet().removeIf(entry -> entry.getValue() < agora);
    }
}
