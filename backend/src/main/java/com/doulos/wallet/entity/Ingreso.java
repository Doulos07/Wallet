// Ingreso.java
package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.TipoMovimiento;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("INGRESO")
@SuperBuilder
@NoArgsConstructor
public class Ingreso extends Transaccion {

    @Override
    public String getTipoMovimiento() {
        return "INGRESO";
    }

    // En Ingreso.java
    @Override
    public Asiento generarAsiento() {
        Asiento asiento = crearAsientoBase();
        // Debita cuenta externa y acredita billetera del usuario
        asiento.agregarLinea(this.getCuentaDestino(), this.getMontoCentavos(), TipoMovimiento.DEBITO);
        asiento.agregarLinea(this.getCuentaOrigen(), this.getMontoCentavos(), TipoMovimiento.CREDITO);
        return asiento;
    }

    @Override
    public Transaccion clonarParaEjecucion(LocalDateTime fechaEjecucion) {
        return Ingreso.builder()
                .montoCentavos(getMontoCentavos())
                .descripcion(getDescripcion())
                .fecha(fechaEjecucion)
                .tercero(getTercero())
                .estadoBancario(EstadoConciliacion.NO_LIQUIDADO)
                .cuentaOrigen(getCuentaOrigen())
                .cuentaDestino(getCuentaDestino())
                .categoria(getCategoria())
                .metodoPago(getMetodoPago())
                .build();
    }
}