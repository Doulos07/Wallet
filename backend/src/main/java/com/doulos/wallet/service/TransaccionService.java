package com.doulos.wallet.service;

import com.doulos.wallet.dto.transaccion.*;
import com.doulos.wallet.entity.*;
import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.EstadoTransaccion;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final CuentaRepository cuentaRepository;
    private final CategoriaRepository categoriaRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final AsientoRepository asientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroExtraRepository registroExtraRepository;

    @Transactional
    public TransaccionResponseDTO registrarGasto(CrearGastoDTO dto, Long usuarioId) {
        Cuenta cuentaOrigen = obtenerCuentaValida(dto.getCuentaOrigenId(), usuarioId);

        Gasto gasto = Gasto.builder()
                .tercero(dto.getTercero())
                .garantiaMeses(dto.getGarantiaMeses())
                .categoria(obtenerCategoriaRef(dto.getCategoriaId()))
                .cuentaDestino(obtenerCuentaExterna(usuarioId))
                .build();

        completarBase(gasto, dto, cuentaOrigen);
        return procesarTransaccion(gasto);
    }

    @Transactional
    public TransaccionResponseDTO registrarIngreso(CrearIngresoDTO dto, Long usuarioId) {
        Cuenta cuentaOrigen = obtenerCuentaValida(dto.getCuentaOrigenId(), usuarioId);

        Ingreso ingreso = Ingreso.builder()
                .tercero(dto.getTercero())
                .categoria(obtenerCategoriaRef(dto.getCategoriaId()))
                .cuentaDestino(obtenerCuentaExterna(usuarioId))
                .build();

        completarBase(ingreso, dto, cuentaOrigen);
        return procesarTransaccion(ingreso);
    }

    @Transactional
    public TransaccionResponseDTO registrarTransferencia(CrearTransferenciaDTO dto, Long usuarioId) {
        if (dto.getCuentaOrigenId().equals(dto.getCuentaDestinoId())) {
            throw new IllegalArgumentException("La cuenta origen y destino no pueden ser la misma.");
        }

        Cuenta cuentaOrigen = obtenerCuentaValida(dto.getCuentaOrigenId(), usuarioId);
        Cuenta cuentaDestino = obtenerCuentaValida(dto.getCuentaDestinoId(), usuarioId);

        Transferencia transferencia = Transferencia.builder()
                .cuentaDestino(cuentaDestino)
                .build();

        completarBase(transferencia, dto, cuentaOrigen);
        return procesarTransaccion(transferencia);
    }

    // Registra el saldo inicial de una cuenta nueva mediante partida doble
    @Transactional
    public void registrarIngresoInicial(Cuenta cuenta, Long montoCentavos, Long usuarioId) {
        Ingreso ingresoInicial = Ingreso.builder()
                .montoCentavos(montoCentavos)
                .descripcion("Saldo inicial")
                .tercero("Apertura de cuenta")
                .fecha(LocalDateTime.now())
                .estadoBancario(EstadoConciliacion.LIQUIDADO)
                .estadoNegocio(EstadoTransaccion.VIGENTE)
                .cuentaOrigen(cuenta)
                .cuentaDestino(obtenerCuentaExterna(usuarioId))
                .build();

        procesarTransaccion(ingresoInicial);
    }

    // Asigna los atributos comunes usando polimorfismo sobre la entidad padre
    private void completarBase(Transaccion tx, TransaccionBaseDTO dto, Cuenta cuentaOrigen) {
        tx.setMontoCentavos(dto.getMontoCentavos());
        tx.setDescripcion(dto.getDescripcion());
        tx.setFecha(Objects.requireNonNullElse(dto.getFecha(), LocalDateTime.now()));
        tx.setEstadoBancario(Objects.requireNonNullElse(dto.getEstadoConciliacion(), EstadoConciliacion.LIQUIDADO));
        tx.setEstadoNegocio(Objects.requireNonNullElse(dto.getEstadoNegocio(), EstadoTransaccion.VIGENTE));
        tx.setCuentaOrigen(cuentaOrigen);
        tx.setMetodoPago(obtenerMetodoPagoRef(dto.getMetodoPagoId()));
    }

    // Genera el asiento contable, actualiza saldos y persiste la transacción
    private TransaccionResponseDTO procesarTransaccion(Transaccion transaccion) {
        Asiento asiento = transaccion.generarAsiento();
        transaccion.setAsiento(asiento);

        asiento.impactarSaldos();

        Transaccion guardada = transaccionRepository.save(transaccion);
        return TransaccionResponseDTO.fromEntity(guardada);
    }

    // Válida existencia, pertenencia al usuario y que no sea la cuenta externa
    private Cuenta obtenerCuentaValida(Long cuentaId, Long usuarioId) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta no encontrada."));

        if (!cuenta.getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("No tenés permiso para operar sobre esta cuenta.");
        }
        if (cuenta.esExterna()) {
            throw new IllegalArgumentException("No se puede operar directamente sobre la cuenta externa.");
        }
        return cuenta;
    }

    private Cuenta obtenerCuentaExterna(Long usuarioId) {
        return cuentaRepository.findByUsuarioIdAndTipo(usuarioId, TipoCuenta.EXTERNA)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta externa del usuario no encontrada."));
    }

    private Categoria obtenerCategoriaRef(Long categoriaId) {
        return categoriaId != null ? categoriaRepository.getReferenceById(categoriaId) : null;
    }

    private MetodoPago obtenerMetodoPagoRef(Long metodoPagoId) {
        return metodoPagoId != null ? metodoPagoRepository.getReferenceById(metodoPagoId) : null;
    }

    @Transactional(readOnly = true)
    public Page<TransaccionResponseDTO> listarTransacciones(
            Long usuarioId,
            Long cuentaId,
            EstadoTransaccion estadoNegocio,
            EstadoConciliacion estadoBancario,
            LocalDate desde,
            LocalDate hasta,
            Pageable pageable) {

        LocalDateTime fechaDesde = (desde != null)
                ? desde.atStartOfDay()
                : LocalDateTime.of(1970, 1, 1, 0, 0);

        LocalDateTime fechaHasta = (hasta != null)
                ? hasta.atTime(LocalTime.MAX)
                : LocalDateTime.of(2100, 12, 31, 23, 59, 59);

        return transaccionRepository
                .buscarPorUsuarioConFiltros(usuarioId, cuentaId, estadoNegocio, estadoBancario, fechaDesde, fechaHasta, pageable)
                .map(TransaccionResponseDTO::fromEntity);
    }

    @Transactional
    public TransaccionResponseDTO anularTransaccion(Long transaccionId, Long usuarioId) {
        Transaccion tx = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada con ID: " + transaccionId));

        if (!tx.getCuentaOrigen().getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("No tenés permiso para anular esta transacción.");
        }

        // Marca la transacción como anulada
        tx.anular();

        // Genera contra-asiento e impacta saldos invertidos solo si estaba autorizada
        if (tx.getAsiento() != null) {
            Asiento contraAsiento = tx.getAsiento().generarContraAsiento();
            contraAsiento.impactarSaldos();
            asientoRepository.save(contraAsiento);
        }

        Transaccion guardada = transaccionRepository.save(tx);
        return TransaccionResponseDTO.fromEntity(guardada);
    }

    private void revertirSaldos(Transaccion tx) {
        Cuenta origen = tx.getCuentaOrigen();
        Long monto = tx.getMontoCentavos();

        switch (tx) {
            case Gasto gasto -> origen.setSaldoCentavos(origen.getSaldoCentavos() + monto);
            case Ingreso ingreso -> origen.setSaldoCentavos(origen.getSaldoCentavos() - monto);
            case Transferencia transferencia -> {
                Cuenta destino = transferencia.getCuentaDestino();
                origen.setSaldoCentavos(origen.getSaldoCentavos() + monto);
                destino.setSaldoCentavos(destino.getSaldoCentavos() - monto);
            }
            default -> {
            }
        }
    }

    @Transactional
    public TransaccionResponseDTO autorizarTransaccion(Long transaccionId, Long usuarioId) {
        Transaccion tx = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new EntityNotFoundException("Transacción no encontrada con ID: " + transaccionId));

        if (!tx.getCuentaOrigen().getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("No tenés permiso para autorizar esta transacción.");
        }
        if (tx.getEstadoNegocio() == EstadoTransaccion.ANULADO) {
            throw new IllegalStateException("No se puede autorizar una transacción anulada.");
        }
        if (tx.getAsiento() != null || tx.getEstadoBancario() != EstadoConciliacion.NO_LIQUIDADO) {
            throw new IllegalStateException("La transacción ya se encuentra autorizada.");
        }
        if (tx.getPagoPlanificado() != null && tx.getPagoPlanificado().obtenerPlantilla() == null) {
            throw new IllegalStateException("No se puede autorizar directamente la plantilla de un pago planificado.");
        }

        // Registra la fecha efectiva en que se aprueba y debita el movimiento
        tx.setFecha(LocalDateTime.now());
        tx.setEstadoBancario(EstadoConciliacion.LIQUIDADO);

        Asiento asiento = tx.generarAsiento();
        tx.setAsiento(asiento);
        asiento.impactarSaldos();

        if (tx instanceof Gasto gasto) {
            registroExtraRepository.findByPagoId(gasto.getId())
                    .forEach(extra -> extra.liquidarCon(gasto));
        }

        Transaccion guardada = transaccionRepository.save(tx);
        return TransaccionResponseDTO.fromEntity(guardada);
    }
}