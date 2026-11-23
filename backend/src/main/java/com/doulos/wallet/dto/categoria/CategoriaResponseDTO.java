package com.doulos.wallet.dto.categoria;

import com.doulos.wallet.entity.Categoria;

public record CategoriaResponseDTO(
        Long id,
        String nombre,
        boolean estado
) {
    public static CategoriaResponseDTO fromEntity(Categoria categoria) {
        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNombre(),
                categoria.isEstado()
        );
    }
}