package com.doulos.wallet.service;

import com.doulos.wallet.dto.cuenta.CrearCuentaDTO;
import com.doulos.wallet.dto.cuenta.CuentaResponseDTO;
import com.doulos.wallet.entity.Cuenta;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.repository.CuentaRepository;
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
    private final TransaccionService transaccionService;

    @Transactional
    public CuentaResponseDTO crearCuenta(CrearCuentaDTO dto, Long usuarioId) {
        if (dto.tipo() == TipoCuenta.EXTERNA || "__CUENTA_EXTERNA__".equalsIgnoreCase(dto.nombre().trim())) {
            throw new IllegalArgumentException("Tipo o nombre de cuenta reservado.");
        }

        // HU-03: El nombre debe ser único por usuario
        if (cuentaRepository.existsByNombreAndUsuarioId(dto.nombre().trim(), usuarioId)) {
            throw new IllegalArgumentException("Ya existe una cuenta con este nombre.");
        }

        if (dto.montoInicialCentavos() != null && dto.montoInicialCentavos() < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo.");
        }

        // Crea un proxy con el ID sin ejecutar un SELECT extra
        Usuario usuario = usuarioRepository.getReferenceById(usuarioId);

        Cuenta cuenta = Cuenta.builder()
                .nombre(dto.nombre().trim())
                .tipo(dto.tipo())
                .usuario(usuario)
                .build();

        Cuenta cuentaGuardada = cuentaRepository.save(cuenta);

        // HU-03: Genera el asiento de apertura contra la cuenta externa
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
        cuenta.setEstado(false);
        cuentaRepository.save(cuenta);
    }

    @Transactional(readOnly = true)
    public List<CuentaResponseDTO> listarCuentasActivas(Long usuarioId) {
        return cuentaRepository.findByUsuarioIdAndTipoNotAndEstadoTrue(usuarioId, TipoCuenta.EXTERNA)
                .stream()
                .map(CuentaResponseDTO::fromEntity)
                .toList();
    }


    // En CuentaService.java
    @Transactional(readOnly = true)
    public Cuenta obtenerCuentaValida(Long cuentaId, Long usuarioId) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta no encontrada."));
        cuenta.validarOperablePor(usuarioId);
        return cuenta;
    }

    @Transactional(readOnly = true)
    public Cuenta obtenerCuentaExterna(Long usuarioId) {
        return cuentaRepository.findByUsuarioIdAndTipo(usuarioId, TipoCuenta.EXTERNA)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta externa del usuario no encontrada."));
    }

}