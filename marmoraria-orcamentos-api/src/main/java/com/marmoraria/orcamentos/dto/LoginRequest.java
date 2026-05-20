package com.marmoraria.orcamentos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Senha e obrigatoria")
    private String senha;

    @NotBlank(message = "Username e obrigatorio")
    private String username;
}
