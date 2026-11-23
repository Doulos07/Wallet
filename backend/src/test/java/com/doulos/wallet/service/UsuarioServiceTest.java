package com.doulos.wallet.service;

import com.doulos.wallet.dto.usuario.UsuarioResponseDTO;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Nested
    @DisplayName("Consulta por ID (Particiones y Fronteras)")
    class ObtenerPorIdTests {

        @Test
        void obtenerUsuarioPorId_CuandoExiste_RetornaDTO() {
            Usuario usuarioMock = Usuario.builder()
                    .id(1L)
                    .nombre("Santiago")
                    .email("santiago@wallet.com")
                    .build();

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));

            UsuarioResponseDTO resultado = usuarioService.obtenerUsuarioId(1L);

            assertNotNull(resultado);
            assertEquals(1L, resultado.id());
            assertEquals("Santiago", resultado.nombre());
            verify(usuarioRepository, times(1)).findById(1L);
        }

        @Test
        void obtenerUsuarioPorId_CuandoNoExisteOIdFrontera_LanzaExcepcion() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
            when(usuarioRepository.findById(0L)).thenReturn(Optional.empty());
            when(usuarioRepository.findById(-1L)).thenReturn(Optional.empty());

            // Partición ID inexistente
            assertThrows(EntityNotFoundException.class, () -> usuarioService.obtenerUsuarioId(99L));
            // Fronteras cero y negativa
            assertThrows(EntityNotFoundException.class, () -> usuarioService.obtenerUsuarioId(0L));
            assertThrows(EntityNotFoundException.class, () -> usuarioService.obtenerUsuarioId(-1L));
        }
    }

    @Nested
    @DisplayName("Listado de Usuarios")
    class ListarUsuariosTests {

        @Test
        void listarUsuarios_ConElementos_RetornaListaMapeada() {
            List<Usuario> listaMock = List.of(
                    Usuario.builder().id(1L).nombre("Santiago").email("santiago@wallet.com").build(),
                    Usuario.builder().id(2L).nombre("Ana").email("ana@wallet.com").build()
            );
            when(usuarioRepository.findAll()).thenReturn(listaMock);

            List<UsuarioResponseDTO> resultado = usuarioService.listarUsuarios();

            assertEquals(2, resultado.size());
            assertEquals("Ana", resultado.get(1).nombre());
        }

        @Test
        void listarUsuarios_FronteraSinRegistros_RetornaListaVacia() {
            when(usuarioRepository.findAll()).thenReturn(Collections.emptyList());

            List<UsuarioResponseDTO> resultado = usuarioService.listarUsuarios();

            assertTrue(resultado.isEmpty());
        }
    }
}