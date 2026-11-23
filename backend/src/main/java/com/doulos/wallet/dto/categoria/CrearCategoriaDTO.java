package com.doulos.wallet.dto.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearCategoriaDTO(
        @NotBlank(message = "El nombre de la categoría es obligatorio.")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres.")
        String nombre
) {
}