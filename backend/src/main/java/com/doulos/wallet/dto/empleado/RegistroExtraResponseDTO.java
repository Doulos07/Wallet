package com.doulos.wallet.dto.empleado;

import com.doulos.wallet.entity.RegistroExtra;
import com.doulos.wallet.entity.enums.EstadoConciliacion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistroExtraResponseDTO(
        Long id,
        LocalDate fecha,
        BigDecimal cantidadHorasExtra,
        String descripcion,
        Long montoCalculadoCentavos,
        EstadoConciliacion estadoLiquidacion
) {
    public static RegistroExtraResponseDTO fromEntity(RegistroExtra r) {
        return new RegistroExtraResponseDTO(
                r.getId(),
                r.getFecha(),
                r.getCantidadHorasExtra(),
                r.getDescripcion(),
                r.getMontoCalculadoCentavos(),
                r.getEstadoLiquidacion()
        );
    }
}