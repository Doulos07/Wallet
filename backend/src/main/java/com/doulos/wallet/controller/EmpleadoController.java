package com.doulos.wallet.controller;

import com.doulos.wallet.dto.empleado.*;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.service.EmpleadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    @PostMapping
    public ResponseEntity<EmpleadoResponseDTO> crear(
            @Valid @RequestBody CrearEmpleadoDTO dto,
            @AuthenticationPrincipal Usuario jefe
    ) {
        EmpleadoResponseDTO creado = empleadoService.crear(dto, jefe);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public ResponseEntity<List<EmpleadoResponseDTO>> listarActivos(
            @AuthenticationPrincipal Usuario jefe
    ) {
        return ResponseEntity.ok(empleadoService.listarActivos(jefe.getId()));
    }

    @GetMapping("/{empleadoId}/detalle")
    public ResponseEntity<EmpleadoDetalleResponseDTO> obtenerDetalle(
            @PathVariable Long empleadoId,
            @AuthenticationPrincipal Usuario jefe
    ) {
        return ResponseEntity.ok(empleadoService.obtenerDetalle(empleadoId, jefe.getId()));
    }

    @PostMapping("/{empleadoId}/extras")
    public ResponseEntity<Void> registrarExtra(
            @PathVariable Long empleadoId,
            @Valid @RequestBody CrearRegistroExtraDTO dto,
            @AuthenticationPrincipal Usuario jefe
    ) {
        empleadoService.registrarExtra(empleadoId, dto, jefe.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{empleadoId}/adelantos")
    public ResponseEntity<Void> registrarAdelanto(
            @PathVariable Long empleadoId,
            @Valid @RequestBody CrearAdelantoSueldoDTO dto,
            @AuthenticationPrincipal Usuario jefe
    ) {
        empleadoService.registrarAdelanto(empleadoId, dto, jefe.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{empleadoId}/desactivar")
    public ResponseEntity<Void> desactivar(
            @PathVariable Long empleadoId,
            @AuthenticationPrincipal Usuario jefe
    ) {
        empleadoService.desactivar(empleadoId, jefe.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{empleadoId}")
    public ResponseEntity<EmpleadoResponseDTO> obtenerPorId(
            @PathVariable Long empleadoId,
            @AuthenticationPrincipal Usuario jefe
    ) {
        return ResponseEntity.ok(empleadoService.obtenerPorId(empleadoId, jefe.getId()));
    }

    @PatchMapping("/{empleadoId}/condiciones")
    public ResponseEntity<EmpleadoResponseDTO> actualizarCondiciones(
            @PathVariable Long empleadoId,
            @Valid @RequestBody ActualizarEmpleadoDTO dto,
            @AuthenticationPrincipal Usuario jefe
    ) {
        return ResponseEntity.ok(empleadoService.actualizarCondiciones(empleadoId, dto, jefe.getId()));
    }
}