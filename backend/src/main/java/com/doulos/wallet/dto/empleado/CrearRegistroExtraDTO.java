package com.doulos.wallet.dto.empleado;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CrearRegistroExtraDTO(
        @NotNull(message = "La fecha es obligatoria.")
        LocalDate fecha,

        @Positive(message = "La cantidad de horas extra debe ser mayor a cero.")
        BigDecimal cantidadHorasExtra,

        String descripcion,

        @Positive(message = "El monto manual debe ser mayor a cero.")
        Long montoManualCentavos
) {
}