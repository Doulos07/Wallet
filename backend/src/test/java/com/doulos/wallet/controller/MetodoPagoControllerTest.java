package com.doulos.wallet.controller;

import com.doulos.wallet.config.SecurityConfig;
import com.doulos.wallet.dto.metodoPago.CrearMetodoPagoDTO;
import com.doulos.wallet.dto.metodoPago.MetodoPagoResponseDTO;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.service.JwtService;
import com.doulos.wallet.service.MetodoPagoService;
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

@WebMvcTest(MetodoPagoController.class)
@Import(SecurityConfig.class)
class MetodoPagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MetodoPagoService metodoPagoService;

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
    @DisplayName("POST /api/metodos-pago")
    class CrearMetodoPagoWebTests {

        @Test
        void crear_ConDatosValidos_Retorna201YJson() throws Exception {
            CrearMetodoPagoDTO dto = new CrearMetodoPagoDTO("Cheque");
            MetodoPagoResponseDTO respuesta = new MetodoPagoResponseDTO(6L, "Cheque", true);

            when(metodoPagoService.crear(any(CrearMetodoPagoDTO.class))).thenReturn(respuesta);

            mockMvc.perform(post("/api/metodos-pago")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(6L))
                    .andExpect(jsonPath("$.nombre").value("Cheque"))
                    .andExpect(jsonPath("$.estado").value(true));
        }

        @Test
        void crear_NombreEnBlancoOSuperaLimite_Retorna400() throws Exception {
            // Frontera nombre en blanco
            mockMvc.perform(post("/api/metodos-pago")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CrearMetodoPagoDTO(""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erroresCampos.nombre").exists());

            // Frontera 81 caracteres
            String nombreLargo = "B".repeat(81);
            mockMvc.perform(post("/api/metodos-pago")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CrearMetodoPagoDTO(nombreLargo))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erroresCampos.nombre").exists());

            verify(metodoPagoService, never()).crear(any());
        }

        @Test
        void crear_NombreDuplicado_Retorna400() throws Exception {
            when(metodoPagoService.crear(any(CrearMetodoPagoDTO.class)))
                    .thenThrow(new IllegalArgumentException("Ya existe un método de pago con el nombre: Efectivo"));

            mockMvc.perform(post("/api/metodos-pago")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CrearMetodoPagoDTO("Efectivo"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("Ya existe un método de pago con el nombre: Efectivo"));
        }

        @Test
        void crear_SinAutenticacion_Retorna403() throws Exception {
            mockMvc.perform(post("/api/metodos-pago")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CrearMetodoPagoDTO("Efectivo"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET y PATCH /api/metodos-pago")
    class ListarYDesactivarWebTests {

        @Test
        void listarActivos_ConYSinElementos_Retorna200() throws Exception {
            MetodoPagoResponseDTO item = new MetodoPagoResponseDTO(1L, "Efectivo", true);
            when(metodoPagoService.listarActivos()).thenReturn(List.of(item));

            mockMvc.perform(get("/api/metodos-pago")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nombre").value("Efectivo"));

            // Frontera lista vacía
            when(metodoPagoService.listarActivos()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/metodos-pago")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void desactivar_Existente_Retorna204() throws Exception {
            mockMvc.perform(patch("/api/metodos-pago/{id}/desactivar", 2L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isNoContent());

            verify(metodoPagoService).desactivar(2L);
        }

        @Test
        void desactivar_Inexistente_Retorna404() throws Exception {
            doThrow(new EntityNotFoundException("Método de pago no encontrado con ID: 99"))
                    .when(metodoPagoService).desactivar(99L);

            mockMvc.perform(patch("/api/metodos-pago/{id}/desactivar", 99L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void listarYDesactivar_SinAutenticacion_Retorna403() throws Exception {
            mockMvc.perform(get("/api/metodos-pago")).andExpect(status().isForbidden());
            mockMvc.perform(patch("/api/metodos-pago/{id}/desactivar", 1L)).andExpect(status().isForbidden());
        }
    }
}