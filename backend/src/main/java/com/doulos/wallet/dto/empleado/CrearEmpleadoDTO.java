package com.doulos.wallet.dto.empleado;

import com.doulos.wallet.entity.Empleado;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.entity.enums.TipoPagoHoraExtra;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Objects;

public record CrearEmpleadoDTO(
        @NotBlank(message = "El nombre es obligatorio.")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio.")
        String apellido,

        @NotBlank(message = "El DNI es obligatorio.")
        String dni,

        @NotNull(message = "El sueldo base es obligatorio.")
        @Positive(message = "El sueldo base debe ser mayor a cero.")
        Long sueldoBaseCentavos,

        @NotNull(message = "Las horas mensuales base son obligatorias.")
        @Positive(message = "Las horas mensuales base deben ser mayores a cero.")
        Integer horasMensualesBase,

        @PositiveOrZero(message = "El valor de hora extra no puede ser negativo.")
        BigDecimal valorHoraExtra,

        @NotNull(message = "El tipo de pago de hora extra es obligatorio.")
        TipoPagoHoraExtra tipoHoraExtra,

        @Min(value = 1, message = "El día de pago debe estar entre 1 y 31.")
        @Max(value = 31, message = "El día de pago debe estar entre 1 y 31.")
        Integer diaPagoSueldo,

        @Min(value = 1, message = "El día de pago debe estar entre 1 y 31.")
        @Max(value = 31, message = "El día de pago debe estar entre 1 y 31.")
        Integer diaPagoExtras,

        @NotNull(message = "La cuenta origen es obligatoria.")
        Long cuentaOrigenId,

        @NotNull(message = "La categoría es obligatoria.")
        Long categoriaId,

        @NotNull(message = "El método de pago es obligatorio.")
        Long metodoPagoId
) {
    public Empleado toEntity(Usuario jefe) {
        return Empleado.builder()
                .nombre(nombre)
                .apellido(apellido)
                .dni(dni)
                .sueldoBaseCentavos(sueldoBaseCentavos)
                .horasMensualesBase(horasMensualesBase)
                .valorHoraExtra(Objects.requireNonNullElse(valorHoraExtra, BigDecimal.ONE))
                .tipoHoraExtra(tipoHoraExtra)
                .diaPagoSueldo(Objects.requireNonNullElse(diaPagoSueldo, 10))
                .diaPagoExtras(Objects.requireNonNullElse(diaPagoExtras, 20))
                .jefe(jefe)
                .build();
    }
}