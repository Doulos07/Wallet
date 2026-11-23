package com.doulos.wallet.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("PAGO_INFINITO")
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public class PagoInfinito extends PagoRecurrencia{

    @Override
    protected boolean haFinalizado() {
        return false;
    }
}
