package com.marmoraria.orcamentos.service;

import com.marmoraria.orcamentos.entity.Usuario;
import com.marmoraria.orcamentos.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class AuthService {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    JwtService jwtService;

    public String autenticarUsuario (String username, String senha) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        if (!senha.equals(usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta");
        }
        return jwtService.gerarToken(username);
    }
}
