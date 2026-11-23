package com.doulos.wallet.entity.enums;

public enum TipoCuenta {
    CUENTA_GENERAL,
    EFECTIVO,
    CUENTA_CORRIENTE,
    TARJETA_CREDITO,
    CUENTA_AHORRO,
    BONO,
    SEGURO,
    INVERSION,
    PRESTAMO,
    HIPOTECA,
    CUENTA_SOBREGIRO,
    EXTERNA;

    public boolean esDebito() {
        return switch (this) {
            // Agrupas todas las cuentas que se comportan como débito (dinero propio)
            case EFECTIVO, CUENTA_CORRIENTE, CUENTA_AHORRO, CUENTA_GENERAL -> true;

            // Agrupas las cuentas de crédito/deuda
            case TARJETA_CREDITO, PRESTAMO, HIPOTECA, CUENTA_SOBREGIRO -> false;

            // Comportamiento por defecto para las demás (BONO, INVERSION, etc.)
            default -> false;
        };
    }
}
