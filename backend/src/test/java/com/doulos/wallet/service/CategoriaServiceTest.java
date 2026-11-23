package com.doulos.wallet.service;

import com.doulos.wallet.dto.categoria.CategoriaResponseDTO;
import com.doulos.wallet.dto.categoria.CrearCategoriaDTO;
import com.doulos.wallet.entity.Categoria;
import com.doulos.wallet.repository.CategoriaRepository;
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
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    @Test
    void crear_NombreValidoConEspacios_LimpiaYGuardaActiva() {
        CrearCategoriaDTO dto = new CrearCategoriaDTO("  Servicios  ");
        when(categoriaRepository.existsByNombreIgnoreCase("Servicios")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoriaResponseDTO resultado = categoriaService.crear(dto);

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());

        assertEquals("Servicios", captor.getValue().getNombre());
        assertTrue(captor.getValue().isEstado());
        assertEquals(1L, resultado.id());
        assertEquals("Servicios", resultado.nombre());
    }

    @Test
    void crear_NombreDuplicado_LanzaIllegalArgumentException() {
        CrearCategoriaDTO dto = new CrearCategoriaDTO("Alimentación");
        when(categoriaRepository.existsByNombreIgnoreCase("Alimentación")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> categoriaService.crear(dto));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void listarActivas_ConYSinResultados_RetornaListaMapeada() {
        Categoria cat = Categoria.builder().id(1L).nombre("Salud").estado(true).build();
        when(categoriaRepository.findByEstadoTrue()).thenReturn(List.of(cat));

        List<CategoriaResponseDTO> resultado = categoriaService.listarActivas();

        assertEquals(1, resultado.size());
        assertEquals("Salud", resultado.get(0).nombre());
        assertTrue(resultado.get(0).estado());

        // Frontera lista vacía
        when(categoriaRepository.findByEstadoTrue()).thenReturn(Collections.emptyList());
        assertTrue(categoriaService.listarActivas().isEmpty());
    }

    @Test
    void desactivar_Existente_CambiaEstadoAFalse() {
        Categoria cat = Categoria.builder().id(5L).nombre("Ocio").estado(true).build();
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(cat));

        categoriaService.desactivar(5L);

        assertFalse(cat.isEstado());
        verify(categoriaRepository).save(cat);
    }

    @Test
    void desactivar_Inexistente_LanzaEntityNotFoundException() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> categoriaService.desactivar(99L));
        verify(categoriaRepository, never()).save(any());
    }
}