package com.doulos.wallet.dto.transaccion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
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
public class CrearTransferenciaDTO extends TransaccionBaseDTO {

    @NotNull(message = "La cuenta destino es obligatoria.")
    private Long cuentaDestinoId;

    @JsonIgnore
    @AssertTrue(message = "La cuenta destino debe ser distinta a la cuenta origen.")
    public boolean isCuentasDistintas() {
        if (getCuentaOrigenId() == null || cuentaDestinoId == null) {
            return true;
        }
        return !getCuentaOrigenId().equals(cuentaDestinoId);
    }
}