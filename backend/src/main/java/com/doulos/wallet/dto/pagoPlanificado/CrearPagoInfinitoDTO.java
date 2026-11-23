// CrearPagoInfinitoDTO.java
package com.doulos.wallet.dto.pagoPlanificado;

import com.doulos.wallet.dto.transaccion.TransaccionPlanificableDTO;
import com.doulos.wallet.entity.PagoInfinito;
import com.doulos.wallet.entity.PagoPlanificado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record CrearPagoInfinitoDTO(
        @NotNull(message = "La fecha de ejecución es obligatoria.")
        LocalDate fecha,

        @Valid
        @NotNull(message = "La plantilla de transacción es obligatoria.")
        TransaccionPlanificableDTO plantilla,

        @NotNull(message = "La frecuencia es obligatoria.")
        ChronoUnit frecuencia
) implements CrearPagoPlanificadoDTO {

    @Override
    public PagoPlanificado toEntity() {
        return PagoInfinito.builder()
                .fecha(fecha)
                .frecuencia(frecuencia)
                .build();
    }
}