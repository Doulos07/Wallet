package com.doulos.wallet.service;

import com.doulos.wallet.dto.metodoPago.CrearMetodoPagoDTO;
import com.doulos.wallet.dto.metodoPago.MetodoPagoResponseDTO;
import com.doulos.wallet.entity.MetodoPago;
import com.doulos.wallet.repository.MetodoPagoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetodoPagoServiceTest {

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    @InjectMocks
    private MetodoPagoService metodoPagoService;

    @Test
    void crear_NombreValidoConEspacios_LimpiaYGuardaActivo() {
        CrearMetodoPagoDTO dto = new CrearMetodoPagoDTO("  Efectivo  ");
        when(metodoPagoRepository.existsByNombreIgnoreCase("Efectivo")).thenReturn(false);
        when(metodoPagoRepository.save(any(MetodoPago.class))).thenAnswer(inv -> {
            MetodoPago mp = inv.getArgument(0);
            mp.setId(2L);
            return mp;
        });

        MetodoPagoResponseDTO resultado = metodoPagoService.crear(dto);

        ArgumentCaptor<MetodoPago> captor = ArgumentCaptor.forClass(MetodoPago.class);
        verify(metodoPagoRepository).save(captor.capture());

        assertEquals("Efectivo", captor.getValue().getNombre());
        assertTrue(captor.getValue().isEstado());
        assertEquals(2L, resultado.id());
        assertEquals("Efectivo", resultado.nombre());
    }

    @Test
    void crear_NombreDuplicado_LanzaIllegalArgumentException() {
        CrearMetodoPagoDTO dto = new CrearMetodoPagoDTO("Tarjeta de Débito");
        when(metodoPagoRepository.existsByNombreIgnoreCase("Tarjeta de Débito")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> metodoPagoService.crear(dto));
        verify(metodoPagoRepository, never()).save(any());
    }

    @Test
    void listarActivos_ConYSinResultados_RetornaListaMapeada() {
        MetodoPago mp = MetodoPago.builder().id(1L).nombre("Efectivo").estado(true).build();
        when(metodoPagoRepository.findByEstadoTrue()).thenReturn(List.of(mp));

        List<MetodoPagoResponseDTO> resultado = metodoPagoService.listarActivos();

        assertEquals(1, resultado.size());
        assertEquals("Efectivo", resultado.get(0).nombre());
        assertTrue(resultado.get(0).estado());

        // Frontera lista vacía
        when(metodoPagoRepository.findByEstadoTrue()).thenReturn(Collections.emptyList());
        assertTrue(metodoPagoService.listarActivos().isEmpty());
    }

    @Test
    void desactivar_Existente_CambiaEstadoAFalse() {
        MetodoPago mp = MetodoPago.builder().id(3L).nombre("QR").estado(true).build();
        when(metodoPagoRepository.findById(3L)).thenReturn(Optional.of(mp));

        metodoPagoService.desactivar(3L);

        assertFalse(mp.isEstado());
        verify(metodoPagoRepository).save(mp);
    }

    @Test
    void desactivar_Inexistente_LanzaEntityNotFoundException() {
        when(metodoPagoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> metodoPagoService.desactivar(99L));
        verify(metodoPagoRepository, never()).save(any());
    }
}