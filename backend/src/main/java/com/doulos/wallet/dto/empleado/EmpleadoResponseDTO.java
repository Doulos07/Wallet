package com.doulos.wallet.dto.empleado;

import com.doulos.wallet.entity.Empleado;
import com.doulos.wallet.entity.enums.TipoPagoHoraExtra;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpleadoResponseDTO(
        Long id,
        String nombre,
        String apellido,
        String dni,
        Long sueldoBaseCentavos,
        Integer horasMensualesBase,
        BigDecimal valorHoraExtra,
        TipoPagoHoraExtra tipoHoraExtra,
        boolean activo,
        LocalDate proximoPagoSueldo
) {
    public static EmpleadoResponseDTO fromEntity(Empleado e) {
        LocalDate proximaFecha = e.getPagoPlanificadoSueldo() != null
                ? e.getPagoPlanificadoSueldo().getFecha()
                : null;

        return new EmpleadoResponseDTO(
                e.getId(),
                e.getNombre(),
                e.getApellido(),
                e.getDni(),
                e.getSueldoBaseCentavos(),
                e.getHorasMensualesBase(),
                e.getValorHoraExtra(),
                e.getTipoHoraExtra(),
                e.isActivo(),
                proximaFecha
        );
    }
}