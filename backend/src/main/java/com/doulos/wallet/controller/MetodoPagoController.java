// com.doulos.wallet.controller.MetodoPagoController.java
package com.doulos.wallet.controller;

import com.doulos.wallet.dto.metodoPago.CrearMetodoPagoDTO;
import com.doulos.wallet.dto.metodoPago.MetodoPagoResponseDTO;
import com.doulos.wallet.service.MetodoPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metodos-pago")
@RequiredArgsConstructor
public class MetodoPagoController {

    private final MetodoPagoService metodoPagoService;

    @PostMapping
    public ResponseEntity<MetodoPagoResponseDTO> crear(@Valid @RequestBody CrearMetodoPagoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(metodoPagoService.crear(dto));
    }

    @GetMapping
    public ResponseEntity<List<MetodoPagoResponseDTO>> listarActivos() {
        return ResponseEntity.ok(metodoPagoService.listarActivos());
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        metodoPagoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}