// com.doulos.wallet.service.MetodoPagoService.java
package com.doulos.wallet.service;

import com.doulos.wallet.dto.metodoPago.CrearMetodoPagoDTO;
import com.doulos.wallet.dto.metodoPago.MetodoPagoResponseDTO;
import com.doulos.wallet.entity.MetodoPago;
import com.doulos.wallet.repository.MetodoPagoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetodoPagoService {

    private final MetodoPagoRepository metodoPagoRepository;

    @Transactional
    public MetodoPagoResponseDTO crear(CrearMetodoPagoDTO dto) {
        String nombreLimpio = dto.nombre().trim();
        if (metodoPagoRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException("Ya existe un método de pago con el nombre: " + nombreLimpio);
        }

        MetodoPago nuevo = MetodoPago.builder()
                .nombre(nombreLimpio)
                .estado(true)
                .build();

        return MetodoPagoResponseDTO.fromEntity(metodoPagoRepository.save(nuevo));
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoResponseDTO> listarActivos() {
        return metodoPagoRepository.findByEstadoTrue()
                .stream()
                .map(MetodoPagoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void desactivar(Long id) {
        MetodoPago metodoPago = metodoPagoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Método de pago no encontrado con ID: " + id));
        metodoPago.setEstado(false);
        metodoPagoRepository.save(metodoPago);
    }
}