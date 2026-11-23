package com.doulos.wallet.dto.pagoPlanificado;

import com.doulos.wallet.dto.transaccion.TransaccionResponseDTO;
import com.doulos.wallet.entity.PagoPlanificado;
import com.doulos.wallet.entity.Transaccion;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record PagoPlanificadoResponseDTO(
        Long id,
        String tipoRecurrencia,
        LocalDate fecha,
        boolean activo,
        ChronoUnit frecuencia,
        LocalDate fechaFin,
        Integer cantidadEventos,
        Integer eventoActual,

        TransaccionResponseDTO transaccion
) {
    public static PagoPlanificadoResponseDTO fromEntity(PagoPlanificado p) {

        Transaccion plantilla = p.obtenerPlantilla();

        return new PagoPlanificadoResponseDTO(
                p.getId(),
                p.getTipoRecurrencia(),
                p.getFecha(),
                p.isActivo(),
                p.getFrecuencia(),
                p.getFechaFin(),
                p.getCantidadEventos(),
                p.getEventoActual(),
                plantilla != null ? TransaccionResponseDTO.fromEntity(plantilla) : null
        );
    }
}