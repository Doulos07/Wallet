package com.doulos.wallet.controller;

import com.doulos.wallet.config.SecurityConfig;
import com.doulos.wallet.dto.categoria.CategoriaResponseDTO;
import com.doulos.wallet.dto.categoria.CrearCategoriaDTO;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.service.CategoriaService;
import com.doulos.wallet.service.JwtService;
import com.doulos.wallet.service.UserDetailsServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoriaController.class)
@Import(SecurityConfig.class)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoriaService categoriaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private Authentication authUsuario;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().id(1L).nombre("Santiago").email("santiago@wallet.com").build();
        authUsuario = new UsernamePasswordAuthenticationToken(usuario, null, List.of());
    }

    @Nested
    @DisplayName("POST /api/categorias")
    class CrearCategoriaWebTests {

        @Test
        void crear_ConDatosValidos_Retorna201YJson() throws Exception {
            CrearCategoriaDTO dto = new CrearCategoriaDTO("Impuestos");
            CategoriaResponseDTO respuesta = new CategoriaResponseDTO(10L, "Impuestos", true);

            when(categoriaService.crear(any(CrearCategoriaDTO.class))).thenReturn(respuesta);

            mockMvc.perform(post("/api/categorias")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.nombre").value("Impuestos"))
                    .andExpect(jsonPath("$.estado").value(true));
        }

        @Test
        void crear_NombreEnBlancoOSuperaLimite_Retorna400() throws Exception {
            // Frontera nombre en blanco
            mockMvc.perform(post("/api/categorias")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CrearCategoriaDTO("   "))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erroresCampos.nombre").exists());

            // Frontera 81 caracteres
            String nombreLargo = "A".repeat(81);
            mockMvc.perform(post("/api/categorias")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CrearCategoriaDTO(nombreLargo))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erroresCampos.nombre").exists());

            verify(categoriaService, never()).crear(any());
        }

        @Test
        void crear_NombreDuplicado_Retorna400() throws Exception {
            when(categoriaService.crear(any(CrearCategoriaDTO.class)))
                    .thenThrow(new IllegalArgumentException("Ya existe una categoría con el nombre: Salud"));

            mockMvc.perform(post("/api/categorias")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CrearCategoriaDTO("Salud"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("Ya existe una categoría con el nombre: Salud"));
        }

        @Test
        void crear_SinAutenticacion_Retorna403() throws Exception {
            mockMvc.perform(post("/api/categorias")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CrearCategoriaDTO("Salud"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET y PATCH /api/categorias")
    class ListarYDesactivarWebTests {

        @Test
        void listarActivas_ConYSinElementos_Retorna200() throws Exception {
            CategoriaResponseDTO item = new CategoriaResponseDTO(1L, "Alimentación", true);
            when(categoriaService.listarActivas()).thenReturn(List.of(item));

            mockMvc.perform(get("/api/categorias")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nombre").value("Alimentación"));

            // Frontera lista vacía
            when(categoriaService.listarActivas()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/categorias")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void desactivar_Existente_Retorna204() throws Exception {
            mockMvc.perform(patch("/api/categorias/{id}/desactivar", 4L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isNoContent());

            verify(categoriaService).desactivar(4L);
        }

        @Test
        void desactivar_Inexistente_Retorna404() throws Exception {
            doThrow(new EntityNotFoundException("Categoría no encontrada con ID: 99"))
                    .when(categoriaService).desactivar(99L);

            mockMvc.perform(patch("/api/categorias/{id}/desactivar", 99L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void listarYDesactivar_SinAutenticacion_Retorna403() throws Exception {
            mockMvc.perform(get("/api/categorias")).andExpect(status().isForbidden());
            mockMvc.perform(patch("/api/categorias/{id}/desactivar", 1L)).andExpect(status().isForbidden());
        }
    }
}