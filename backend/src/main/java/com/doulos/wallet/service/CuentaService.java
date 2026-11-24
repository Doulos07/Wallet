package com.doulos.wallet.service;

import com.doulos.wallet.dto.cuenta.CrearCuentaDTO;
import com.doulos.wallet.dto.cuenta.CuentaResponseDTO;
import com.doulos.wallet.entity.Cuenta;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.repository.CuentaRepository;
import com.doulos.wallet.repository.TransaccionRepository;
import com.doulos.wallet.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TransaccionRepository transaccionRepository;
    private final TransaccionService transaccionService;

    @Transactional
    public CuentaResponseDTO crearCuenta(CrearCuentaDTO dto, Long usuarioId) {
        String nombreLimpio = dto.nombre().trim();

        if (dto.tipo() == TipoCuenta.EXTERNA || "__CUENTA_EXTERNA__".equalsIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException("Tipo o nombre de cuenta reservado.");
        }

        // Válida unicidad solo entre las cuentas activas del usuario
        if (cuentaRepository.existsByNombreIgnoreCaseAndUsuarioIdAndEstadoTrue(nombreLimpio, usuarioId)) {
            throw new IllegalArgumentException("Ya existe una cuenta activa con este nombre.");
        }

        if (dto.montoInicialCentavos() != null && dto.montoInicialCentavos() < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo.");
        }

        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);

        Cuenta cuenta = Cuenta.builder()
                .nombre(nombreLimpio)
                .tipo(dto.tipo())
                .usuario(usuario)
                .build();

        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);

        // Genera el asiento de apertura contra la cuenta externa
        if (dto.montoInicialCentavos() != null && dto.montoInicialCentavos() > 0) {
            transaccionService.registrarIngresoInicial(cuentaGuardada, dto.montoInicialCentavos(), usuarioId);
        }

        return CuentaResponseDTO.fromEntity(cuentaGuardada);
    }

    @Transactional(readOnly = true)
    public CuentaResponseDTO obtenerPorId(Long id, Long usuarioId) {
        Cuenta cuenta = obtenerCuentaValida(id, usuarioId);
        return CuentaResponseDTO.fromEntity(cuenta);
    }

    @Transactional
    public void desactivar(Long id, Long usuarioId) {
        Cuenta cuenta = obtenerCuentaValida(id, usuarioId);

        boolean tieneMovimientos = transaccionRepository
                .existsByCuentaOrigenIdOrCuentaDestinoId(id, id);

        if (tieneMovimientos) {
            // Conserva el historial contable marcando baja lógica
            cuenta.setEstado(false);
            cuentaRepository.save(cuenta);
        } else {
            // Elimina físicamente si nunca tuvo transacciones
            cuentaRepository.delete(cuenta);
        }
    }

    @Transactional(readOnly = true)
    public List<CuentaResponseDTO> listarCuentasActivas(Long usuarioId) {
        return cuentaRepository.findByUsuarioIdAndTipoNotAndEstadoTrue(usuarioId, TipoCuenta.EXTERNA)
                .stream()
                .map(CuentaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Cuenta obtenerCuentaValida(Long cuentaId, Long usuarioId) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta no encontrada."));
        cuenta.validarOperablePor(usuarioId);
        return cuenta;
    }

    @Transactional
    public Cuenta obtenerCuentaExterna(Long usuarioId) {
        return cuentaRepository.findByUsuarioIdAndTipo(usuarioId, TipoCuenta.EXTERNA)
                .orElseGet(() -> {
                    Usuario usuario = usuarioRepository.getReferenceById(usuarioId);
                    return cuentaRepository.save(
                            Cuenta.builder()
                                    .nombre("__CUENTA_EXTERNA__")
                                    .tipo(TipoCuenta.EXTERNA)
                                    .usuario(usuario)
                                    .build()
                    );
                });
    }
}