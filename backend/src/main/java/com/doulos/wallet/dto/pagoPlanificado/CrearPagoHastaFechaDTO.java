// CrearPagoHastaFechaDTO.java
package com.doulos.wallet.dto.pagoPlanificado;

import com.doulos.wallet.dto.transaccion.TransaccionPlanificableDTO;
import com.doulos.wallet.entity.PagoHastaFecha;
import com.doulos.wallet.entity.PagoPlanificado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record CrearPagoHastaFechaDTO(
        @NotNull(message = "La fecha de ejecución es obligatoria.")
        LocalDate fecha,

        @Valid
        @NotNull(message = "La plantilla de transacción es obligatoria.")
        TransaccionPlanificableDTO plantilla,

        @NotNull(message = "La frecuencia es obligatoria.")
        ChronoUnit frecuencia,

        @NotNull(message = "La fecha de fin es obligatoria.")
        @Future(message = "La fecha de fin debe ser posterior a hoy.")
        LocalDate fechaFin
) implements CrearPagoPlanificadoDTO {

    @AssertTrue(message = "La fecha de fin debe ser igual o posterior a la fecha de inicio.")
    public boolean isRangoFechasValido() {
        if (fecha == null || fechaFin == null) {
            return true;
        }
        return !fechaFin.isBefore(fecha);
    }

    @Override
    public PagoPlanificado toEntity() {
        return PagoHastaFecha.builder()
                .fecha(fecha)
                .frecuencia(frecuencia)
                .fechaFin(fechaFin)
                .build();
    }
}