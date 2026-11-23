package com.doulos.wallet.dto.pagoPlanificado;

import com.doulos.wallet.dto.transaccion.TransaccionPlanificableDTO;
import com.doulos.wallet.entity.PagoPlanificado;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipoRecurrencia")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CrearPagoUnicoDTO.class, name = "UNICO"),
        @JsonSubTypes.Type(value = CrearPagoInfinitoDTO.class, name = "INFINITO"),
        @JsonSubTypes.Type(value = CrearPagoHastaFechaDTO.class, name = "HASTA_FECHA"),
        @JsonSubTypes.Type(value = CrearPagoPorEventoDTO.class, name = "POR_EVENTO")
})
public sealed interface CrearPagoPlanificadoDTO
        permits CrearPagoUnicoDTO, CrearPagoInfinitoDTO, CrearPagoHastaFechaDTO, CrearPagoPorEventoDTO {

    LocalDate fecha();

    TransaccionPlanificableDTO plantilla();

    PagoPlanificado toEntity();
}