// com.doulos.wallet.service.CategoriaService.java
package com.doulos.wallet.service;

import com.doulos.wallet.dto.categoria.CategoriaResponseDTO;
import com.doulos.wallet.dto.categoria.CrearCategoriaDTO;
import com.doulos.wallet.entity.Categoria;
import com.doulos.wallet.repository.CategoriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional
    public CategoriaResponseDTO crear(CrearCategoriaDTO dto) {
        String nombreLimpio = dto.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre: " + nombreLimpio);
        }

        Categoria nueva = Categoria.builder()
                .nombre(nombreLimpio)
                .estado(true)
                .build();

        return CategoriaResponseDTO.fromEntity(categoriaRepository.save(nueva));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listarActivas() {
        return categoriaRepository.findByEstadoTrue()
                .stream()
                .map(CategoriaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void desactivar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada con ID: " + id));
        categoria.setEstado(false);
        categoriaRepository.save(categoria);
    }
}