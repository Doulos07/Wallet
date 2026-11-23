package com.doulos.wallet.dto.empleado;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CrearAdelantoSueldoDTO(
        @NotNull(message = "La fecha es obligatoria.")
        LocalDate fecha,

        @NotNull(message = "El monto es obligatorio.")
        @Positive(message = "El monto debe ser mayor a cero.")
        Long montoCentavos,

        String motivo,

        @NotNull(message = "La cuenta origen es obligatoria.")
        Long cuentaOrigenId,

        @NotNull(message = "La categoría es obligatoria.")
        Long categoriaId,

        @NotNull(message = "El método de pago es obligatorio.")
        Long metodoPagoId
) {
}