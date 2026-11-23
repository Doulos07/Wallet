package com.doulos.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("PAGO_POR_EVENTO")
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PagoPorEvento extends PagoRecurrencia{

    @Column(name = "cantidad_eventos")
    private int cantidadEventos;

    @Builder.Default
    @Column(name = "evento_actual")
    private int eventoActual = 1;

    public Integer getCantidadEventos(){
        return this.cantidadEventos;
    }

    public Integer getEventoActual(){
        return this.eventoActual;
    }

    @Override
    protected void avanzar() {
        super.avanzar();
        this.eventoActual++;
    }

    @Override
    protected boolean haFinalizado() {
        return this.eventoActual > this.cantidadEventos;
    }
}
