// CrearPagoUnicoDTO.java
package com.doulos.wallet.dto.pagoPlanificado;

import com.doulos.wallet.dto.transaccion.TransaccionPlanificableDTO;
import com.doulos.wallet.entity.PagoPlanificado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CrearPagoUnicoDTO(
        @NotNull(message = "La fecha de ejecución es obligatoria.")
        LocalDate fecha,

        @Valid
        @NotNull(message = "La plantilla de transacción es obligatoria.")
        TransaccionPlanificableDTO plantilla
) implements CrearPagoPlanificadoDTO {

    @Override
    public PagoPlanificado toEntity() {
        return PagoPlanificado.builder().fecha(fecha).build();
    }
}