package com.doulos.wallet.dto.transaccion;

import com.doulos.wallet.entity.*;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipoMovimiento")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CrearGastoDTO.class, name = "GASTO"),
        @JsonSubTypes.Type(value = CrearIngresoDTO.class, name = "INGRESO")
})
public sealed interface TransaccionPlanificableDTO
        permits CrearGastoDTO, CrearIngresoDTO {

    Long getCuentaOrigenId();
    Long getCategoriaId();
    Long getMetodoPagoId();
    LocalDateTime getFecha();

    // Construye la transacción moldé sin asiento contable
    Transaccion toPlantillaEntity(Cuenta origen, Cuenta externa, Categoria categoria, MetodoPago metodoPago);
}