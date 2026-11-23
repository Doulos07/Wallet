package com.doulos.wallet.service;

import com.doulos.wallet.dto.usuario.CrearUsuarioDTO;
import com.doulos.wallet.dto.usuario.UsuarioResponseDTO;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService { // Service del negocio

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioResponseDTO crearUsuario(CrearUsuarioDTO dto) {

        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Ya existe un usuario con este email.");
        }


        Usuario usuario = Usuario.builder()
                .nombre(dto.nombre())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Mapeamos manualmente a DTO para devolver SOLO lo público
        return new UsuarioResponseDTO(
                usuarioGuardado.getId(),
                usuarioGuardado.getNombre(),
                usuarioGuardado.getEmail()
        );
    }

    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(user -> new UsuarioResponseDTO(
                        user.getId(),
                        user.getNombre(),
                        user.getEmail()
                ))
                .toList();
    }

    public UsuarioResponseDTO obtenerUsuarioId (Long id){
        Optional<Usuario> usuario = usuarioRepository.findById(id);

        if (usuario.isPresent()) {
            return  new UsuarioResponseDTO(
                    usuario.get().getId(),
                    usuario.get().getNombre(),
                    usuario.get().getEmail());
        } else {
            throw new EntityNotFoundException("No existe el usuario con el id: " + id);
        }
    }
}