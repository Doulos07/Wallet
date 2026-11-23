// com.doulos.wallet.controller.CategoriaController.java
package com.doulos.wallet.controller;

import com.doulos.wallet.dto.categoria.CategoriaResponseDTO;
import com.doulos.wallet.dto.categoria.CrearCategoriaDTO;
import com.doulos.wallet.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> crear(@Valid @RequestBody CrearCategoriaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(dto));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> listarActivas() {
        return ResponseEntity.ok(categoriaService.listarActivas());
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        categoriaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}