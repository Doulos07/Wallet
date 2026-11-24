package com.doulos.wallet.dto.empleado;

import com.doulos.wallet.entity.AdelantoSueldo;
import com.doulos.wallet.entity.Empleado;
import com.doulos.wallet.entity.PagoPlanificado;
import com.doulos.wallet.entity.RegistroExtra;
import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.TipoPagoHoraExtra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record EmpleadoDetalleResponseDTO(
        Long id,
        String nombre,
        String apellido,
        String dni,
        Long sueldoBaseCentavos,
        Integer horasMensualesBase,
        BigDecimal valorHoraExtra,
        TipoPagoHoraExtra tipoHoraExtra,
        Integer diaPagoSueldo,
        Integer diaPagoExtras,
        boolean activo,
        LocalDate proximoPagoSueldo,
        LocalDate proximoPagoExtras,
        Long totalAdelantosPendientesCentavos,
        Long totalExtrasPendientesCentavos,
        List<AdelantoSueldoResponseDTO> adelantos,
        List<RegistroExtraResponseDTO> registrosExtra
) {
    public static EmpleadoDetalleResponseDTO fromEntity(Empleado e) {
        LocalDate proximaFechaSueldo = e.getPagoPlanificadoSueldo() != null
                ? e.getPagoPlanificadoSueldo().getFecha()
                : null;

        LocalDate proximaFechaExtras = e.obtenerPagoExtrasActivo()
                .map(PagoPlanificado::getFecha)
                .orElse(null);

        long adelantosPendientes = e.getAdelantoSueldos().stream()
                .filter(a -> !a.isDescontado())
                .mapToLong(AdelantoSueldo::getMontoCentavos)
                .sum();

        long extrasPendientes = e.getRegistroExtras().stream()
                .filter(r -> r.getEstadoLiquidacion() != EstadoConciliacion.LIQUIDADO)
                .mapToLong(RegistroExtra::getMontoCalculadoCentavos)
                .sum();

        List<AdelantoSueldoResponseDTO> adelantosOrdenados = e.getAdelantoSueldos().stream()
                .sorted(Comparator.comparing(AdelantoSueldo::getFecha).reversed()
                        .thenComparing(AdelantoSueldo::getId, Comparator.reverseOrder()))
                .map(AdelantoSueldoResponseDTO::fromEntity)
                .toList();

        List<RegistroExtraResponseDTO> extrasOrdenados = e.getRegistroExtras().stream()
                .sorted(Comparator.comparing(RegistroExtra::getFecha).reversed()
                        .thenComparing(RegistroExtra::getId, Comparator.reverseOrder()))
                .map(RegistroExtraResponseDTO::fromEntity)
                .toList();

        return new EmpleadoDetalleResponseDTO(
                e.getId(),
                e.getNombre(),
                e.getApellido(),
                e.getDni(),
                e.getSueldoBaseCentavos(),
                e.getHorasMensualesBase(),
                e.getValorHoraExtra(),
                e.getTipoHoraExtra(),
                e.getDiaPagoSueldo(),
                e.getDiaPagoExtras(),
                e.isActivo(),
                proximaFechaSueldo,
                proximaFechaExtras,
                adelantosPendientes,
                extrasPendientes,
                adelantosOrdenados,
                extrasOrdenados
        );
    }
}