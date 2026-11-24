package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.TipoMovimiento;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("GASTO")
@SuperBuilder
@NoArgsConstructor
public class Gasto extends Transaccion {

    @Override
    public String getTipoMovimiento() {
        return "GASTO";
    }

    // En Gasto.java
    @Override
    public Asiento generarAsiento() {
        Asiento asiento = crearAsientoBase();
        // Debita billetera del usuario y acredita cuenta externa
        asiento.agregarLinea(this.getCuentaOrigen(), this.getMontoCentavos(), TipoMovimiento.DEBITO);
        asiento.agregarLinea(this.getCuentaDestino(), this.getMontoCentavos(), TipoMovimiento.CREDITO);
        return asiento;
    }
    
    @Override
    public Transaccion clonarParaEjecucion(LocalDateTime fechaEjecucion) {
        return Gasto.builder()
                .montoCentavos(getMontoCentavos())
                .descripcion(getDescripcion())
                .fecha(fechaEjecucion)
                .tercero(getTercero())
                .garantiaMeses(getGarantiaMeses())
                .estadoBancario(EstadoConciliacion.NO_LIQUIDADO)
                .cuentaOrigen(getCuentaOrigen())
                .cuentaDestino(getCuentaDestino())
                .categoria(getCategoria())
                .metodoPago(getMetodoPago())
                .build();
    }
}
