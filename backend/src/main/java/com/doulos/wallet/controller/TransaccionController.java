package com.doulos.wallet.controller;

import com.doulos.wallet.dto.transaccion.CrearGastoDTO;
import com.doulos.wallet.dto.transaccion.CrearIngresoDTO;
import com.doulos.wallet.dto.transaccion.CrearTransferenciaDTO;
import com.doulos.wallet.dto.transaccion.TransaccionResponseDTO;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.EstadoTransaccion;
import com.doulos.wallet.service.TransaccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transacciones")
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionService transaccionService;


    @GetMapping
    public ResponseEntity<Page<TransaccionResponseDTO>> listarTransacciones(
            @RequestParam(required = false) Long cuentaId,
            @RequestParam(required = false) EstadoTransaccion estadoNegocio,
            @RequestParam(required = false) EstadoConciliacion estadoBancario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Usuario usuario) {

        Page<TransaccionResponseDTO> pagina = transaccionService.listarTransacciones(
                usuario.getId(), cuentaId, estadoNegocio, estadoBancario, desde, hasta, pageable);
        return ResponseEntity.ok(pagina);
    }

    @PostMapping("/gastos")
    public ResponseEntity<TransaccionResponseDTO> registrarGasto(
            @Valid @RequestBody CrearGastoDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        TransaccionResponseDTO response = transaccionService.registrarGasto(dto, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/ingresos")
    public ResponseEntity<TransaccionResponseDTO> registrarIngreso(
            @Valid @RequestBody CrearIngresoDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        TransaccionResponseDTO response = transaccionService.registrarIngreso(dto, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transferencias")
    public ResponseEntity<TransaccionResponseDTO> registrarTransferencia(
            @Valid @RequestBody CrearTransferenciaDTO dto,
            @AuthenticationPrincipal Usuario usuario) {
        TransaccionResponseDTO response = transaccionService.registrarTransferencia(dto, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/anular")
    public ResponseEntity<TransaccionResponseDTO> anularTransaccion(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        TransaccionResponseDTO anulada = transaccionService.anularTransaccion(id, usuario.getId());
        return ResponseEntity.ok(anulada);
    }
}