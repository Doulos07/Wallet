package com.doulos.wallet.service;

import com.doulos.wallet.dto.usuario.CrearUsuarioDTO;
import com.doulos.wallet.dto.usuario.UsuarioResponseDTO;
import com.doulos.wallet.entity.Cuenta;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.repository.CuentaRepository;
import com.doulos.wallet.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
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

        // Crea la cuenta de contrapartida para asientos de ingresos, gastos y saldos iniciales
        cuentaRepository.save(
                Cuenta.builder()
                        .nombre("__CUENTA_EXTERNA__")
                        .tipo(TipoCuenta.EXTERNA)
                        .usuario(usuarioGuardado)
                        .build()
        );

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

    public UsuarioResponseDTO obtenerUsuarioId(Long id) {
        Optional<Usuario> usuario = usuarioRepository.findById(id);

        if (usuario.isPresent()) {
            return new UsuarioResponseDTO(
                    usuario.get().getId(),
                    usuario.get().getNombre(),
                    usuario.get().getEmail());
        } else {
            throw new EntityNotFoundException("No existe el usuario con el id: " + id);
        }
    }
}