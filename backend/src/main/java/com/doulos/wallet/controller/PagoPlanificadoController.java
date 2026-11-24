package com.doulos.wallet.controller;

import com.doulos.wallet.dto.pagoPlanificado.CrearPagoPlanificadoDTO;
import com.doulos.wallet.dto.pagoPlanificado.PagoPlanificadoResponseDTO;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.service.PagoPlanificadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pagos-planificados")
@RequiredArgsConstructor
public class PagoPlanificadoController {

    private final PagoPlanificadoService pagoPlanificadoService;

    // Crea un pago planificado (único o recurrente) según el tipoRecurrencia recibido
    @PostMapping
    public ResponseEntity<PagoPlanificadoResponseDTO> crear(
            @Valid @RequestBody CrearPagoPlanificadoDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        PagoPlanificadoResponseDTO creado = pagoPlanificadoService.crear(dto, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // Devuelve los pagos planificados activos del usuario autenticado
    @GetMapping
    public ResponseEntity<List<PagoPlanificadoResponseDTO>> listarActivos(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(pagoPlanificadoService.listarActivos(usuario.getId()));
    }

    // Desactiva un pago planificado para detener futuras ejecuciones
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        pagoPlanificadoService.desactivar(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/procesar-pendientes")
    public ResponseEntity<Integer> procesarPendientes(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDate fechaObjetivo = (fecha != null) ? fecha : LocalDate.now();
        return ResponseEntity.ok(pagoPlanificadoService.ejecutarPendientes(fechaObjetivo));
    }
}