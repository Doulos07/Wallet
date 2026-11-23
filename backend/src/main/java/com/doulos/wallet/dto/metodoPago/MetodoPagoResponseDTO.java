package com.doulos.wallet.dto.metodoPago;

import com.doulos.wallet.entity.MetodoPago;

public record MetodoPagoResponseDTO(
        Long id,
        String nombre,
        boolean estado
) {
    public static MetodoPagoResponseDTO fromEntity(MetodoPago metodoPago) {
        return new MetodoPagoResponseDTO(
                metodoPago.getId(),
                metodoPago.getNombre(),
                metodoPago.isEstado()
        );
    }
}