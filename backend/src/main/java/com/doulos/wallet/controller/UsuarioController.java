package com.doulos.wallet.controller;

import com.doulos.wallet.dto.auth.AuthResponseDTO;
import com.doulos.wallet.dto.usuario.CrearUsuarioDTO;
import com.doulos.wallet.dto.auth.LoginUsuarioDTO;
import com.doulos.wallet.dto.usuario.UsuarioResponseDTO;
import com.doulos.wallet.service.JwtService;
import com.doulos.wallet.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponseDTO> registrarUsuario(@Valid @RequestBody CrearUsuarioDTO dto) {
        UsuarioResponseDTO nuevoUsuario = usuarioService.crearUsuario(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> iniciarSesion(@Valid @RequestBody LoginUsuarioDTO dto) {
        Authentication autenticacionExitosa = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password())
        );

        // Extraemos el UserDetails (tu entidad Usuario) y generamos el JWT[cite: 2, 3]
        UserDetails usuario = (UserDetails) autenticacionExitosa.getPrincipal();
        String jwt = jwtService.generarToken(usuario);

        return ResponseEntity.ok(new AuthResponseDTO(jwt));
    }


    @GetMapping("")
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerUsuarios() {
        List<UsuarioResponseDTO> usuarios = usuarioService.listarUsuarios();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuario (@PathVariable Long id) {
        UsuarioResponseDTO usuario = usuarioService.obtenerUsuarioId(id);
        return ResponseEntity.ok(usuario);
    }


}
