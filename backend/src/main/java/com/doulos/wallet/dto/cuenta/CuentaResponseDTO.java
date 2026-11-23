package com.doulos.wallet.dto.cuenta;

import com.doulos.wallet.entity.Cuenta;
import com.doulos.wallet.entity.enums.TipoCuenta;

public record CuentaResponseDTO(
        Long id,
        String nombre,
        TipoCuenta tipo,
        Long saldoCentavos,
        boolean estado
) {
    // Convierte la entidad a DTO sin tocar relaciones lazy
    public static CuentaResponseDTO fromEntity(Cuenta cuenta) {
        return new CuentaResponseDTO(
                cuenta.getId(),
                cuenta.getNombre(),
                cuenta.getTipo(),
                cuenta.getSaldoCentavos(),
                cuenta.isEstado()
        );
    }
}