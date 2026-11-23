package com.doulos.wallet.controller;

import com.doulos.wallet.config.SecurityConfig;
import com.doulos.wallet.dto.cuenta.CrearCuentaDTO;
import com.doulos.wallet.dto.cuenta.CuentaResponseDTO;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.service.CuentaService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CuentaController.class)
@Import(SecurityConfig.class)
class CuentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CuentaService cuentaService;

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
    @DisplayName("POST /api/cuentas/registrar")
    class RegistrarCuentaTests {

        @Test
        void registrarCuenta_FronteraSaldoCero_Retorna200YCuenta() throws Exception {
            CrearCuentaDTO dto = new CrearCuentaDTO("Efectivo", TipoCuenta.EFECTIVO, 0L);
            CuentaResponseDTO respuesta = new CuentaResponseDTO(10L, "Efectivo", TipoCuenta.EFECTIVO, 0L, true);

            when(cuentaService.crearCuenta(any(CrearCuentaDTO.class), eq(1L))).thenReturn(respuesta);

            mockMvc.perform(post("/api/cuentas/registrar")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.nombre").value("Efectivo"))
                    .andExpect(jsonPath("$.saldoCentavos").value(0L))
                    .andExpect(jsonPath("$.estado").value(true));
        }

        @Test
        void registrarCuenta_DatosInvalidosEnDto_Retorna400ConDetalleCampos() throws Exception {
            CrearCuentaDTO dtoInvalido = new CrearCuentaDTO("  ", null, -1L);

            mockMvc.perform(post("/api/cuentas/registrar")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoInvalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erroresCampos.nombre").exists())
                    .andExpect(jsonPath("$.erroresCampos.tipo").exists())
                    .andExpect(jsonPath("$.erroresCampos.montoInicialCentavos").exists());

            verify(cuentaService, never()).crearCuenta(any(), any());
        }

        @Test
        void registrarCuenta_JsonMalformado_Retorna400SinLlamarAlService() throws Exception {
            mockMvc.perform(post("/api/cuentas/registrar")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{json-invalido}"))
                    .andExpect(status().isBadRequest());

            verify(cuentaService, never()).crearCuenta(any(), any());
        }

        @Test
        void registrarCuenta_SinAutenticacion_Retorna403() throws Exception {
            CrearCuentaDTO dto = new CrearCuentaDTO("Efectivo", TipoCuenta.EFECTIVO, 0L);

            mockMvc.perform(post("/api/cuentas/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/cuentas y GET /api/cuentas/{id}")
    class ObtenerCuentaTests {

        @Test
        void listarCuentasActivas_ConYSinResultados_Retorna200() throws Exception {
            CuentaResponseDTO item = new CuentaResponseDTO(10L, "Mercado Pago", TipoCuenta.CUENTA_GENERAL, 50_000L, true);
            when(cuentaService.listarCuentasActivas(1L)).thenReturn(List.of(item));

            mockMvc.perform(get("/api/cuentas")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nombre").value("Mercado Pago"));

            when(cuentaService.listarCuentasActivas(1L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/cuentas")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void obtenerCuenta_Existente_Retorna200() throws Exception {
            CuentaResponseDTO respuesta = new CuentaResponseDTO(10L, "Mercado Pago", TipoCuenta.CUENTA_GENERAL, 50_000L, true);
            when(cuentaService.obtenerPorId(10L, 1L)).thenReturn(respuesta);

            mockMvc.perform(get("/api/cuentas/{id}", 10L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.saldoCentavos").value(50_000L))
                    .andExpect(jsonPath("$.estado").value(true));
        }

        @Test
        void obtenerCuenta_DeOtroUsuario_Retorna403() throws Exception {
            when(cuentaService.obtenerPorId(99L, 1L)).thenThrow(new AccessDeniedException("Sin permiso"));

            mockMvc.perform(get("/api/cuentas/{id}", 99L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void obtenerCuenta_SinAutenticacion_Retorna403() throws Exception {
            mockMvc.perform(get("/api/cuentas/{id}", 10L))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/cuentas/{id}/desactivar")
    class DesactivarCuentaTests {

        @Test
        void desactivarCuenta_Propia_Retorna204() throws Exception {
            mockMvc.perform(patch("/api/cuentas/{id}/desactivar", 10L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isNoContent());

            verify(cuentaService).desactivar(10L, 1L);
        }

        @Test
        void desactivarCuenta_Inexistente_Retorna404() throws Exception {
            doThrow(new EntityNotFoundException("Cuenta no encontrada."))
                    .when(cuentaService).desactivar(404L, 1L);

            mockMvc.perform(patch("/api/cuentas/{id}/desactivar", 404L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void desactivarCuenta_DeOtroUsuario_Retorna403() throws Exception {
            doThrow(new AccessDeniedException("Sin permiso"))
                    .when(cuentaService).desactivar(99L, 1L);

            mockMvc.perform(patch("/api/cuentas/{id}/desactivar", 99L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isForbidden());
        }
    }
}