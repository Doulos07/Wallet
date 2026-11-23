package com.doulos.wallet.dto.pagoPlanificado;

import com.doulos.wallet.dto.transaccion.TransaccionPlanificableDTO;
import com.doulos.wallet.entity.PagoPlanificado;
import com.doulos.wallet.entity.PagoPorEvento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record CrearPagoPorEventoDTO(
        @NotNull(message = "La fecha de ejecución es obligatoria.")
        LocalDate fecha,

        @Valid
        @NotNull(message = "La plantilla de transacción es obligatoria.")
        TransaccionPlanificableDTO plantilla,

        @NotNull(message = "La frecuencia es obligatoria.")
        ChronoUnit frecuencia,

        @NotNull(message = "La cantidad de eventos es obligatoria.")
        @Min(value = 1, message = "La cantidad de eventos debe ser al menos 1.")
        Integer cantidadEventos
) implements CrearPagoPlanificadoDTO {

    @Override
    public PagoPlanificado toEntity() {
        return PagoPorEvento.builder()
                .fecha(fecha)
                .frecuencia(frecuencia)
                .cantidadEventos(cantidadEventos)
                .eventoActual(0)
                .build();
    }
}