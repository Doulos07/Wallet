package com.doulos.wallet.controller;

import com.doulos.wallet.config.SecurityConfig;
import com.doulos.wallet.dto.pagoPlanificado.CrearPagoPlanificadoDTO;
import com.doulos.wallet.dto.pagoPlanificado.CrearPagoPorEventoDTO;
import com.doulos.wallet.dto.pagoPlanificado.PagoPlanificadoResponseDTO;
import com.doulos.wallet.dto.transaccion.CrearGastoDTO;
import com.doulos.wallet.entity.Cuenta;
import com.doulos.wallet.entity.Gasto;
import com.doulos.wallet.entity.PagoPorEvento;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.service.JwtService;
import com.doulos.wallet.service.PagoPlanificadoService;
import com.doulos.wallet.service.UserDetailsServiceImpl;
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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PagoPlanificadoController.class)
@Import(SecurityConfig.class)
class PagoPlanificadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PagoPlanificadoService pagoPlanificadoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private Usuario usuario;
    private Authentication authUsuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).nombre("Santiago").email("santiago@wallet.com").build();
        authUsuario = new UsernamePasswordAuthenticationToken(usuario, null, List.of());
    }

    @Nested
    @DisplayName("POST /api/pagos-planificados")
    class CrearPagoPlanificadoTests {

        @Test
        void crear_ConDatosValidos_Retorna201YJson() throws Exception {
            LocalDate fecha = LocalDate.of(2026, 12, 1);
            CrearGastoDTO plantillaDto = CrearGastoDTO.builder()
                    .montoCentavos(150_000L).descripcion("Internet").cuentaOrigenId(10L).categoriaId(5L).metodoPagoId(2L).build();
            CrearPagoPorEventoDTO dto = new CrearPagoPorEventoDTO(fecha, plantillaDto, ChronoUnit.MONTHS, 3);

            PagoPlanificadoResponseDTO respuesta = construirRespuestaSimulada(100L, fecha);
            when(pagoPlanificadoService.crear(any(CrearPagoPlanificadoDTO.class), eq(1L))).thenReturn(respuesta);

            mockMvc.perform(post("/api/pagos-planificados")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100L))
                    .andExpect(jsonPath("$.activo").value(true));
        }

        @Test
        void crear_ConCamposObligatoriosNulos_Retorna400PorValidacion() throws Exception {
            // Cuerpo vacío viola @NotNull en fecha y plantilla
            mockMvc.perform(post("/api/pagos-planificados")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(pagoPlanificadoService, never()).crear(any(), any());
        }

        @Test
        void crear_SinAutenticacion_Retorna403() throws Exception {
            mockMvc.perform(post("/api/pagos-planificados")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET y PATCH /api/pagos-planificados")
    class ListarYDesactivarTests {

        @Test
        void listarActivos_ConYSinElementos_Retorna200() throws Exception {
            PagoPlanificadoResponseDTO item = construirRespuestaSimulada(100L, LocalDate.of(2026, 12, 1));
            when(pagoPlanificadoService.listarActivos(1L)).thenReturn(List.of(item));

            mockMvc.perform(get("/api/pagos-planificados")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(100L));

            // Frontera lista vacía
            when(pagoPlanificadoService.listarActivos(1L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/pagos-planificados")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void desactivar_Propietario_Retorna204() throws Exception {
            mockMvc.perform(patch("/api/pagos-planificados/{id}/desactivar", 50L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isNoContent());

            verify(pagoPlanificadoService).desactivar(50L, 1L);
        }

        @Test
        void desactivar_DeOtroUsuario_Retorna403() throws Exception {
            doThrow(new AccessDeniedException("Sin permiso"))
                    .when(pagoPlanificadoService).desactivar(50L, 1L);

            mockMvc.perform(patch("/api/pagos-planificados/{id}/desactivar", 50L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isForbidden());
        }
    }

    private PagoPlanificadoResponseDTO construirRespuestaSimulada(Long id, LocalDate fecha) {
        Cuenta origen = Cuenta.builder().id(10L).nombre("Banco").tipo(TipoCuenta.CUENTA_GENERAL).usuario(usuario).build();
        Gasto plantilla = Gasto.builder()
                .montoCentavos(150_000L).descripcion("Internet").fecha(fecha.atStartOfDay()).cuentaOrigen(origen).build();
        PagoPorEvento pago = PagoPorEvento.builder()
                .id(id).fecha(fecha).activo(true).frecuencia(ChronoUnit.MONTHS).cantidadEventos(3).eventoActual(1).build();
        pago.agregarTransaccion(plantilla);
        return PagoPlanificadoResponseDTO.fromEntity(pago);
    }
}