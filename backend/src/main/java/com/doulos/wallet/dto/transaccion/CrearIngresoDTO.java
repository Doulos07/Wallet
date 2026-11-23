package com.doulos.wallet.dto.transaccion;

import com.doulos.wallet.entity.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public final class CrearIngresoDTO extends TransaccionBaseDTO implements TransaccionPlanificableDTO {

    @NotNull(message = "La categoría es obligatoria.")
    private Long categoriaId;

    @Override
    public Transaccion toPlantillaEntity(Cuenta origen, Cuenta externa, Categoria cat, MetodoPago mp) {
        return Ingreso.builder()
                .montoCentavos(getMontoCentavos())
                .descripcion(getDescripcion())
                .fecha(getFecha())
                .tercero(getTercero())
                .cuentaOrigen(origen)
                .cuentaDestino(externa)
                .categoria(cat)
                .metodoPago(mp)
                .build();
    }
}