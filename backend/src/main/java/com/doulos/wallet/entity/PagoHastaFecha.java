package com.doulos.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("PAGO_HASTA_FECHA")
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PagoHastaFecha extends  PagoRecurrencia{

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Override
    protected boolean haFinalizado() {
        return this.getFecha().isAfter(this.fechaFin);
    }
}
