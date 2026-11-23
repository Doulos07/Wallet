package com.doulos.wallet.entity.enums;

import com.doulos.wallet.entity.Empleado;
import java.math.BigDecimal;
import java.math.RoundingMode;

public enum TipoPagoHoraExtra {

    MULTIPLICADOR_HORA {
        @Override
        public long calcularMontoCentavos(Empleado empleado, BigDecimal cantidadHoras, Long montoManualCentavos) {
            validarHoras(cantidadHoras);
            if (empleado.getHorasMensualesBase() == null || empleado.getHorasMensualesBase() <= 0) {
                throw new IllegalArgumentException("Las horas mensuales base deben ser mayores a cero.");
            }
            return BigDecimal.valueOf(empleado.getSueldoBaseCentavos())
                    .multiply(empleado.getValorHoraExtra())
                    .multiply(cantidadHoras)
                    .divide(BigDecimal.valueOf(empleado.getHorasMensualesBase()), 0, RoundingMode.HALF_UP)
                    .longValueExact();
        }
    },

    MONTO_FIJO_HORA {
        @Override
        public long calcularMontoCentavos(Empleado empleado, BigDecimal cantidadHoras, Long montoManualCentavos) {
            validarHoras(cantidadHoras);
            return empleado.getValorHoraExtra()
                    .multiply(cantidadHoras)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        }
    },

    POR_TRABAJO_REALIZADO {
        @Override
        public long calcularMontoCentavos(Empleado empleado, BigDecimal cantidadHoras, Long montoManualCentavos) {
            if (montoManualCentavos == null || montoManualCentavos <= 0) {
                throw new IllegalArgumentException("El monto manual debe ser mayor a cero.");
            }
            return montoManualCentavos;
        }
    };

    private static void validarHoras(BigDecimal cantidadHoras) {
        if (cantidadHoras == null || cantidadHoras.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad de horas extra debe ser mayor a cero.");
        }
    }

    public abstract long calcularMontoCentavos(Empleado empleado, BigDecimal cantidadHoras, Long montoManualCentavos);
}