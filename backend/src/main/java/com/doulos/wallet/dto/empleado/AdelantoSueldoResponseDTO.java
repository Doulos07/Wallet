package com.doulos.wallet.dto.empleado;

import com.doulos.wallet.entity.AdelantoSueldo;

import java.time.LocalDate;

public record AdelantoSueldoResponseDTO(
        Long id,
        LocalDate fecha,
        Long montoCentavos,
        String motivo,
        boolean descontado
) {
    public static AdelantoSueldoResponseDTO fromEntity(AdelantoSueldo a) {
        return new AdelantoSueldoResponseDTO(
                a.getId(),
                a.getFecha(),
                a.getMontoCentavos(),
                a.getMotivo(),
                a.isDescontado()
        );
    }
}