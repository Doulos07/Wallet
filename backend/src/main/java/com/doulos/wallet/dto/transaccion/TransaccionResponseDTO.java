package com.doulos.wallet.dto.transaccion;

import com.doulos.wallet.entity.Cuenta;
import com.doulos.wallet.entity.Transaccion;
import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.EstadoTransaccion;

import java.time.LocalDateTime;

public record TransaccionResponseDTO(
        Long id,
        String tipoMovimiento,
        Long montoCentavos,
        String descripcion,
        LocalDateTime fecha,
        String tercero,
        Integer garantiaMeses,
        EstadoConciliacion estadoBancario,
        EstadoTransaccion estadoNegocio,
        Long cuentaOrigenId,
        String cuentaOrigenNombre,
        Long cuentaDestinoId,
        String cuentaDestinoNombre,
        Long categoriaId,
        Long metodoPagoId
) {
    public static TransaccionResponseDTO fromEntity(Transaccion tx) {
        Cuenta destino = tx.getCuentaDestino();
        // Oculta la cuenta externa para que solo las transferencias expongan destino
        boolean esDestinoVisible = destino != null && !destino.esExterna();

        return new TransaccionResponseDTO(
                tx.getId(),
                tx.getTipoMovimiento(),
                tx.getMontoCentavos(),
                tx.getDescripcion(),
                tx.getFecha(),
                tx.getTercero(),
                tx.getGarantiaMeses(),
                tx.getEstadoBancario(),
                tx.getEstadoNegocio(),
                tx.getCuentaOrigen().getId(),
                tx.getCuentaOrigen().getNombre(),
                esDestinoVisible ? destino.getId() : null,
                esDestinoVisible ? destino.getNombre() : null,
                tx.getCategoria() != null ? tx.getCategoria().getId() : null,
                tx.getMetodoPago() != null ? tx.getMetodoPago().getId() : null
        );
    }
}