package com.doulos.wallet.dto.cuenta;

import com.doulos.wallet.entity.enums.TipoCuenta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CrearCuentaDTO(
        @NotBlank(message = "El nombre de la cuenta es obligatorio.")
        String nombre,

        @NotNull(message = "El tipo de cuenta es obligatorio.")
        TipoCuenta tipo,

        @NotNull(message = "El monto inicial es obligatorio.")
        @PositiveOrZero(message = "El monto inicial no puede ser negativo.")
        Long montoInicialCentavos
) {
}