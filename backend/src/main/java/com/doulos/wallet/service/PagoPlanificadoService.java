package com.doulos.wallet.service;

import com.doulos.wallet.dto.pagoPlanificado.CrearPagoPlanificadoDTO;
import com.doulos.wallet.dto.pagoPlanificado.PagoPlanificadoResponseDTO;
import com.doulos.wallet.dto.transaccion.TransaccionPlanificableDTO;
import com.doulos.wallet.entity.*;
import com.doulos.wallet.repository.CategoriaRepository;
import com.doulos.wallet.repository.MetodoPagoRepository;
import com.doulos.wallet.repository.PagoPlanificadoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PagoPlanificadoService {

    private final PagoPlanificadoRepository pagoPlanificadoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final CategoriaRepository categoriaRepository;
    private final CuentaService cuentaService;

    @Transactional
    public PagoPlanificadoResponseDTO crear(CrearPagoPlanificadoDTO dto, Long usuarioId) {
        TransaccionPlanificableDTO plantillaDto = dto.plantilla();

        Cuenta origen = cuentaService.obtenerCuentaValida(plantillaDto.getCuentaOrigenId(), usuarioId);
        Cuenta externa = cuentaService.obtenerCuentaExterna(usuarioId);
        Categoria categoria = categoriaRepository.findById(plantillaDto.getCategoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada."));
        MetodoPago metodoPago = metodoPagoRepository.findById(plantillaDto.getMetodoPagoId())
                .orElseThrow(() -> new EntityNotFoundException("Método de pago no encontrado."));

        // Construye la transacción plantilla sin asiento contable
        Transaccion plantilla = plantillaDto.toPlantillaEntity(origen, externa, categoria, metodoPago);
        plantilla.setFecha(Objects.requireNonNullElse(plantillaDto.getFecha(), dto.fecha().atStartOfDay()));

        // Instancia el pago planificado y persiste ambos por cascada
        PagoPlanificado pago = dto.toEntity();
        pago.setActivo(true);
        pago.agregarTransaccion(plantilla);

        PagoPlanificado guardado = pagoPlanificadoRepository.save(pago);
        return PagoPlanificadoResponseDTO.fromEntity(guardado);
    }

    @Transactional(readOnly = true)
    public List<PagoPlanificadoResponseDTO> listarActivos(Long usuarioId) {
        return pagoPlanificadoRepository.findActivosConDetalleByUsuarioId(usuarioId)
                .stream()
                .map(PagoPlanificadoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void desactivar(Long pagoId, Long usuarioId) {
        PagoPlanificado pago = pagoPlanificadoRepository.findById(pagoId)
                .orElseThrow(() -> new EntityNotFoundException("Pago planificado no encontrado."));

        pago.validarPertenencia(usuarioId);
        pago.setActivo(false);
    }

    @Transactional
    public int ejecutarPendientes(LocalDate hoy) {
        List<PagoPlanificado> ejecutables = pagoPlanificadoRepository.findEjecutablesHasta(hoy);
        ejecutables.forEach(pago -> pago.ejecutar(hoy));
        return ejecutables.size();
    }
}