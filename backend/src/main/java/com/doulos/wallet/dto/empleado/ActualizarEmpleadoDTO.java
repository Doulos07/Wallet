package com.doulos.wallet.dto.empleado;

import com.doulos.wallet.entity.enums.TipoPagoHoraExtra;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ActualizarEmpleadoDTO(
        @NotNull(message = "El sueldo base es obligatorio.")
        @Positive(message = "El sueldo base debe ser mayor a cero.")
        Long sueldoBaseCentavos,

        @NotNull(message = "Las horas mensuales base son obligatorias.")
        @Positive(message = "Las horas mensuales base deben ser mayores a cero.")
        Integer horasMensualesBase,

        @NotNull(message = "El valor de hora extra es obligatorio.")
        @PositiveOrZero(message = "El valor de hora extra no puede ser negativo.")
        BigDecimal valorHoraExtra,

        @NotNull(message = "El tipo de pago de hora extra es obligatorio.")
        TipoPagoHoraExtra tipoHoraExtra,

        @Min(value = 1, message = "El día de pago debe estar entre 1 y 31.")
        @Max(value = 31, message = "El día de pago debe estar entre 1 y 31.")
        Integer diaPagoSueldo,

        @Min(value = 1, message = "El día de pago debe estar entre 1 y 31.")
        @Max(value = 31, message = "El día de pago debe estar entre 1 y 31.")
        Integer diaPagoExtras
) {
}