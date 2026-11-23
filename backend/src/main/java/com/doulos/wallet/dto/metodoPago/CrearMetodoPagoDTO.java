package com.doulos.wallet.dto.metodoPago;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearMetodoPagoDTO(
        @NotBlank(message = "El nombre del método de pago es obligatorio.")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres.")
        String nombre
) {
}