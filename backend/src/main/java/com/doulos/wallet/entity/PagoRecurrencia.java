package com.doulos.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.temporal.ChronoUnit;

@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public abstract class PagoRecurrencia extends PagoPlanificado {

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ChronoUnit frecuencia;


    @Override
    public void actualizarTrasEjecucion() {
        this.avanzar();
        if (this.haFinalizado()) {
            super.actualizarTrasEjecucion();
        }
    }

    protected void avanzar() {
        this.setFecha(this.getFecha().plus(1, this.frecuencia));
    }

    protected abstract boolean haFinalizado();

}