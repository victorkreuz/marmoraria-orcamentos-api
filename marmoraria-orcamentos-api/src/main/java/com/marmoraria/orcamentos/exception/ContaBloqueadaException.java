package com.marmoraria.orcamentos.exception;

import org.springframework.security.core.AuthenticationException;

public class ContaBloqueadaException extends AuthenticationException {
    public ContaBloqueadaException(String message) {
        super(message);
    }
}