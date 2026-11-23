package com.doulos.wallet.controller;

import com.doulos.wallet.config.SecurityConfig;
import com.doulos.wallet.dto.empleado.*;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.entity.enums.TipoPagoHoraExtra;
import com.doulos.wallet.service.EmpleadoService;
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
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmpleadoController.class)
@Import(SecurityConfig.class)
class EmpleadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmpleadoService empleadoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private Usuario jefe;
    private Authentication authJefe;

    @BeforeEach
    void setUp() {
        jefe = Usuario.builder().id(1L).nombre("Santiago").email("santiago@wallet.com").build();
        authJefe = new UsernamePasswordAuthenticationToken(jefe, null, List.of());
    }

    @Nested
    @DisplayName("POST /api/empleados (Alta y Validaciones @Valid)")
    class CrearEmpleadoWebTests {

        @Test
        void crear_ConDatosValidos_Retorna201YJson() throws Exception {
            CrearEmpleadoDTO dto = new CrearEmpleadoDTO(
                    "Juan", "Pérez", "35111222", 90_000_000L, 144,
                    BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA,
                    10, 20, 10L, 5L, 2L
            );

            EmpleadoResponseDTO respuestaMock = new EmpleadoResponseDTO(
                    7L, "Juan", "Pérez", "35111222", 90_000_000L, 144,
                    BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA, true, LocalDate.of(2026, 12, 10)
            );

            when(empleadoService.crear(any(CrearEmpleadoDTO.class), any(Usuario.class))).thenReturn(respuestaMock);

            mockMvc.perform(post("/api/empleados")
                            .with(authentication(authJefe))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(7L))
                    .andExpect(jsonPath("$.nombre").value("Juan"))
                    .andExpect(jsonPath("$.sueldoBaseCentavos").value(90_000_000L));
        }

        @Test
        void crear_ConFronterasInvalidasEnDto_Retorna400SinLlamarAlService() throws Exception {
            // Nombre en blanco, sueldo 0 y día de pago 32 (fuera de rango 1..31)
            CrearEmpleadoDTO dtoInvalido = new CrearEmpleadoDTO(
                    "", "Pérez", "35111222", 0L, 144,
                    BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA,
                    32, 0, 10L, 5L, 2L
            );

            mockMvc.perform(post("/api/empleados")
                            .with(authentication(authJefe))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoInvalido)))
                    .andExpect(status().isBadRequest());

            verify(empleadoService, never()).crear(any(), any());
        }

        @Test
        void crear_SinAutenticacion_Retorna401O403() throws Exception {
            CrearEmpleadoDTO dto = new CrearEmpleadoDTO(
                    "Juan", "Pérez", "35111222", 90_000_000L, 144,
                    BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA,
                    10, 20, 10L, 5L, 2L
            );

            mockMvc.perform(post("/api/empleados")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("GET /api/empleados y Operaciones de Nómina")
    class OperacionesEmpleadoWebTests {

        @Test
        void listarActivos_Retorna200YLista() throws Exception {
            EmpleadoResponseDTO item = new EmpleadoResponseDTO(
                    7L, "Juan", "Pérez", "35111222", 90_000_000L, 144,
                    BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA, true, LocalDate.of(2026, 12, 10)
            );
            when(empleadoService.listarActivos(1L)).thenReturn(List.of(item));

            mockMvc.perform(get("/api/empleados")
                            .with(authentication(authJefe)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].dni").value("35111222"));
        }

        @Test
        void registrarExtra_Valido_Retorna201() throws Exception {
            CrearRegistroExtraDTO dto = new CrearRegistroExtraDTO(
                    LocalDate.of(2026, 11, 21), new BigDecimal("2.5"), "Guardia", null
            );

            mockMvc.perform(post("/api/empleados/{empleadoId}/extras", 7L)
                            .with(authentication(authJefe))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());

            verify(empleadoService).registrarExtra(eq(7L), any(CrearRegistroExtraDTO.class), eq(1L));
        }

        @Test
        void registrarAdelanto_MontoCeroInvalido_Retorna400() throws Exception {
            // Frontera inválida: montoCentavos = 0 viola @Positive
            CrearAdelantoSueldoDTO dtoInvalido = new CrearAdelantoSueldoDTO(
                    LocalDate.of(2026, 11, 21), 0L, "Adelanto", 10L, 5L, 2L
            );

            mockMvc.perform(post("/api/empleados/{empleadoId}/adelantos", 7L)
                            .with(authentication(authJefe))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoInvalido)))
                    .andExpect(status().isBadRequest());

            verify(empleadoService, never()).registrarAdelanto(any(), any(), any());
        }

        @Test
        void desactivar_Propietario_Retorna204() throws Exception {
            mockMvc.perform(patch("/api/empleados/{empleadoId}/desactivar", 7L)
                            .with(authentication(authJefe))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(empleadoService).desactivar(7L, 1L);
        }

        @Test
        void desactivar_EmpleadoAjeno_Retorna403() throws Exception {
            doThrow(new AccessDeniedException("Sin permiso"))
                    .when(empleadoService).desactivar(7L, 1L);

            mockMvc.perform(patch("/api/empleados/{empleadoId}/desactivar", 7L)
                            .with(authentication(authJefe))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void obtenerPorId_Propietario_Retorna200() throws Exception {
            EmpleadoResponseDTO item = new EmpleadoResponseDTO(
                    7L, "Juan", "Pérez", "35111222", 90_000_000L, 144,
                    BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA, true, LocalDate.of(2026, 12, 10)
            );
            when(empleadoService.obtenerPorId(7L, 1L)).thenReturn(item);

            mockMvc.perform(get("/api/empleados/{empleadoId}", 7L)
                            .with(authentication(authJefe)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(7L))
                    .andExpect(jsonPath("$.nombre").value("Juan"));
        }

        @Test
        void obtenerPorId_Inexistente_Retorna404() throws Exception {
            when(empleadoService.obtenerPorId(404L, 1L))
                    .thenThrow(new EntityNotFoundException("Empleado no encontrado."));

            mockMvc.perform(get("/api/empleados/{empleadoId}", 404L)
                            .with(authentication(authJefe)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void actualizarCondiciones_DatosValidos_Retorna200() throws Exception {
            ActualizarEmpleadoDTO dto = new ActualizarEmpleadoDTO(
                    110_000_000L, 160, new BigDecimal("1.5"), TipoPagoHoraExtra.MULTIPLICADOR_HORA, 5, 25
            );
            EmpleadoResponseDTO respuesta = new EmpleadoResponseDTO(
                    7L, "Juan", "Pérez", "35111222", 110_000_000L, 160,
                    new BigDecimal("1.5"), TipoPagoHoraExtra.MULTIPLICADOR_HORA, true, LocalDate.of(2026, 12, 5)
            );

            when(empleadoService.actualizarCondiciones(eq(7L), any(ActualizarEmpleadoDTO.class), eq(1L)))
                    .thenReturn(respuesta);

            mockMvc.perform(patch("/api/empleados/{empleadoId}/condiciones", 7L)
                            .with(authentication(authJefe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sueldoBaseCentavos").value(110_000_000L))
                    .andExpect(jsonPath("$.horasMensualesBase").value(160));
        }

        @Test
        void actualizarCondiciones_SueldoCeroInvalido_Retorna400() throws Exception {
            ActualizarEmpleadoDTO dtoInvalido = new ActualizarEmpleadoDTO(
                    0L, 160, BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA, 10, 20
            );

            mockMvc.perform(patch("/api/empleados/{empleadoId}/condiciones", 7L)
                            .with(authentication(authJefe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dtoInvalido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erroresCampos.sueldoBaseCentavos").exists());

            verify(empleadoService, never()).actualizarCondiciones(any(), any(), any());
        }
    }
}