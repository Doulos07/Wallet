package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.TipoMovimiento;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("TRANSFERENCIA")
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public class Transferencia extends Transaccion {

    @Override
    public String getTipoMovimiento() {
        return "TRANSFERENCIA";
    }

    @Override
    public Asiento generarAsiento() {
        Asiento asiento = crearAsientoBase();
        // Debita en origen y acredita en destino
        asiento.agregarLinea(this.getCuentaOrigen(), this.getMontoCentavos(), TipoMovimiento.DEBITO);
        asiento.agregarLinea(this.getCuentaDestino(), this.getMontoCentavos(), TipoMovimiento.CREDITO);
        return asiento;
    }
}