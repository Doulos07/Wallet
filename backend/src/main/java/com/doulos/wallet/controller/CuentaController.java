package com.doulos.wallet.controller;

import com.doulos.wallet.dto.cuenta.CrearCuentaDTO;
import com.doulos.wallet.dto.cuenta.CuentaResponseDTO;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.service.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @PostMapping("/registrar")
    public ResponseEntity<CuentaResponseDTO> registrarCuenta(
            @Valid @RequestBody CrearCuentaDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        CuentaResponseDTO cuenta = cuentaService.crearCuenta(dto, usuario.getId());
        return ResponseEntity.ok(cuenta);
    }

    @GetMapping
    public ResponseEntity<List<CuentaResponseDTO>> listarCuentasActivas(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(cuentaService.listarCuentasActivas(usuario.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaResponseDTO> obtenerCuenta(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        CuentaResponseDTO cuenta = cuentaService.obtenerPorId(id, usuario.getId());
        return ResponseEntity.ok(cuenta);
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivarCuenta(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        cuentaService.desactivar(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}