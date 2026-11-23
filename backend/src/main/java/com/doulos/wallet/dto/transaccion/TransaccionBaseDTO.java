package com.doulos.wallet.dto.transaccion;

import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.EstadoTransaccion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class TransaccionBaseDTO {

    @NotNull(message = "El monto es obligatorio.")
    @Positive(message = "El monto debe ser mayor a cero.")
    private Long montoCentavos;

    private String descripcion;
    private LocalDateTime fecha;
    private String tercero;

    @PositiveOrZero(message = "Los meses de garantía no pueden ser negativos.")
    private Integer garantiaMeses;

    private EstadoConciliacion estadoConciliacion;
    private EstadoTransaccion estadoNegocio;

    @NotNull(message = "La cuenta origen es obligatoria.")
    private Long cuentaOrigenId;

    @NotNull(message = "El método de pago es obligatorio.")
    private Long metodoPagoId;
}