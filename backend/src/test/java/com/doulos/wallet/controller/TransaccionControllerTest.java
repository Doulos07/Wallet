package com.doulos.wallet.controller;

import com.doulos.wallet.config.SecurityConfig;
import com.doulos.wallet.dto.transaccion.CrearGastoDTO;
import com.doulos.wallet.dto.transaccion.CrearIngresoDTO;
import com.doulos.wallet.dto.transaccion.CrearTransferenciaDTO;
import com.doulos.wallet.dto.transaccion.TransaccionResponseDTO;
import com.doulos.wallet.entity.*;
import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.EstadoTransaccion;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.service.JwtService;
import com.doulos.wallet.service.TransaccionService;
import com.doulos.wallet.service.UserDetailsServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@WebMvcTest(TransaccionController.class)
@Import(SecurityConfig.class)
class TransaccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransaccionService transaccionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private Authentication authUsuario;
    private Cuenta cuentaOrigen;
    private Cuenta cuentaDestino;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().id(1L).nombre("Santiago").email("santiago@wallet.com").build();
        authUsuario = new UsernamePasswordAuthenticationToken(usuario, null, List.of());

        cuentaOrigen = Cuenta.builder().id(10L).nombre("Mercado Pago").tipo(TipoCuenta.CUENTA_GENERAL).usuario(usuario).build();
        cuentaDestino = Cuenta.builder().id(20L).nombre("Efectivo").tipo(TipoCuenta.EFECTIVO).usuario(usuario).build();
    }

    @Nested
    @DisplayName("GET /api/transacciones")
    class ListarTransaccionesWebTests {

        @Test
        void listarTransacciones_SinFiltros_UsaPaginacionPorDefectoYRetorna200() throws Exception {
            // Partición equivalente: consulta sin filtros opcionales
            Gasto gasto = Gasto.builder()
                    .id(100L).montoCentavos(5_000L).descripcion("Almuerzo")
                    .fecha(LocalDateTime.now()).cuentaOrigen(cuentaOrigen).build();
            Page<TransaccionResponseDTO> pagina = new PageImpl<>(List.of(TransaccionResponseDTO.fromEntity(gasto)));

            when(transaccionService.listarTransacciones(eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(pagina);

            mockMvc.perform(get("/api/transacciones")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(100L));

            // Verifica valores límite por defecto: página 0, tamaño 20 y orden fecha DESC
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(transaccionService).listarTransacciones(eq(1L), isNull(), isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture());

            Pageable pageableUsado = pageableCaptor.getValue();
            assertEquals(0, pageableUsado.getPageNumber());
            assertEquals(20, pageableUsado.getPageSize());
            assertEquals(Sort.Direction.DESC, pageableUsado.getSort().getOrderFor("fecha").getDirection());
        }

        @Test
        void listarTransacciones_ConTodosLosFiltrosValidos_ParseaParametrosYRetorna200() throws Exception {
            // Partición equivalente: combinación completa de filtros válidos
            EstadoConciliacion estadoBancario = EstadoConciliacion.values()[0];
            Page<TransaccionResponseDTO> pagina = new PageImpl<>(Collections.emptyList());

            when(transaccionService.listarTransacciones(
                    eq(1L),
                    eq(10L),
                    eq(EstadoTransaccion.ANULADO),
                    eq(estadoBancario),
                    eq(LocalDate.of(2026, 11, 1)),
                    eq(LocalDate.of(2026, 11, 30)),
                    any(Pageable.class)
            )).thenReturn(pagina);

            mockMvc.perform(get("/api/transacciones")
                            .param("cuentaId", "10")
                            .param("estadoNegocio", EstadoTransaccion.ANULADO.name())
                            .param("estadoBancario", estadoBancario.name())
                            .param("desde", "2026-11-01")
                            .param("hasta", "2026-11-30")
                            .param("page", "1")
                            .param("size", "5")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk());
        }

        @Test
        void listarTransacciones_FronteraResultadoVacio_Retorna200ConListaVacia() throws Exception {
            // Caso de borde: 0 elementos encontrados
            Page<TransaccionResponseDTO> paginaVacia = new PageImpl<>(Collections.emptyList());

            when(transaccionService.listarTransacciones(eq(1L), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(paginaVacia);

            mockMvc.perform(get("/api/transacciones")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void listarTransacciones_FronteraMismoDiaDesdeYHasta_Retorna200() throws Exception {
            // Caso de borde: límite inferior y superior del rango iguales
            LocalDate diaExacto = LocalDate.of(2026, 11, 21);
            Page<TransaccionResponseDTO> pagina = new PageImpl<>(Collections.emptyList());

            when(transaccionService.listarTransacciones(eq(1L), isNull(), isNull(), isNull(), eq(diaExacto), eq(diaExacto), any(Pageable.class)))
                    .thenReturn(pagina);

            mockMvc.perform(get("/api/transacciones")
                            .param("desde", "2026-11-21")
                            .param("hasta", "2026-11-21")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk());
        }

        @Test
        void listarTransacciones_ParametroConFormatoInvalido_Retorna400() throws Exception {
            // Partición inválida: fecha o enum malformado en query params
            mockMvc.perform(get("/api/transacciones")
                            .param("desde", "fecha-invalida")
                            .with(authentication(authUsuario)))
                    .andExpect(status().isBadRequest());

            verify(transaccionService, never()).listarTransacciones(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void listarTransacciones_SinAutenticacion_Retorna403() throws Exception {
            // Partición inválida: petición anónima
            mockMvc.perform(get("/api/transacciones"))
                    .andExpect(status().isForbidden());
        }
    }


    @Nested
    @DisplayName("POST /api/transacciones/gastos")
    class GastosWebTests {

        @Test
        void registrarGasto_FronteraMontoMinimo_Retorna201() throws Exception {
            CrearGastoDTO dto = CrearGastoDTO.builder()
                    .montoCentavos(1L)
                    .descripcion("Chicle")
                    .cuentaOrigenId(10L)
                    .categoriaId(5L)
                    .metodoPagoId(2L)
                    .build();

            Gasto gasto = Gasto.builder()
                    .id(100L).montoCentavos(1L).descripcion("Chicle").fecha(LocalDateTime.now()).cuentaOrigen(cuentaOrigen).build();
            when(transaccionService.registrarGasto(any(CrearGastoDTO.class), eq(1L)))
                    .thenReturn(TransaccionResponseDTO.fromEntity(gasto));

            mockMvc.perform(post("/api/transacciones/gastos")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipoMovimiento").value("GASTO"))
                    .andExpect(jsonPath("$.montoCentavos").value(1L));
        }

        @Test
        void registrarGasto_CuentaAjena_Retorna403() throws Exception {
            CrearGastoDTO dto = CrearGastoDTO.builder()
                    .montoCentavos(5_000L)
                    .cuentaOrigenId(99L)
                    .categoriaId(5L)
                    .metodoPagoId(2L)
                    .build();
            when(transaccionService.registrarGasto(any(CrearGastoDTO.class), eq(1L)))
                    .thenThrow(new AccessDeniedException("Cuenta ajena"));

            mockMvc.perform(post("/api/transacciones/gastos")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.mensaje").value("Cuenta ajena"));
        }

        @Test
        void registrarGasto_SinCategoriaNiMetodoPago_Retorna400ConDetalleCampos() throws Exception {
            CrearGastoDTO dto = CrearGastoDTO.builder()
                    .montoCentavos(1_000L)
                    .cuentaOrigenId(10L)
                    .build();

            mockMvc.perform(post("/api/transacciones/gastos")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erroresCampos.categoriaId").exists())
                    .andExpect(jsonPath("$.erroresCampos.metodoPagoId").exists());

            verify(transaccionService, never()).registrarGasto(any(), any());
        }

        @Test
        void registrarGasto_SinAutenticacion_Retorna403() throws Exception {
            mockMvc.perform(post("/api/transacciones/gastos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/transacciones/ingresos")
    class IngresosWebTests {

        @Test
        void registrarIngreso_Valido_Retorna201() throws Exception {
            CrearIngresoDTO dto = CrearIngresoDTO.builder()
                    .montoCentavos(50_000L)
                    .descripcion("Freelance")
                    .cuentaOrigenId(10L)
                    .categoriaId(5L)
                    .metodoPagoId(2L)
                    .build();

            Ingreso ingreso = Ingreso.builder()
                    .id(101L).montoCentavos(50_000L).descripcion("Freelance").fecha(LocalDateTime.now()).cuentaOrigen(cuentaOrigen).build();
            when(transaccionService.registrarIngreso(any(CrearIngresoDTO.class), eq(1L)))
                    .thenReturn(TransaccionResponseDTO.fromEntity(ingreso));

            mockMvc.perform(post("/api/transacciones/ingresos")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipoMovimiento").value("INGRESO"))
                    .andExpect(jsonPath("$.montoCentavos").value(50_000L));
        }

        @Test
        void registrarIngreso_JsonInvalido_Retorna400() throws Exception {
            mockMvc.perform(post("/api/transacciones/ingresos")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("corrupto"))
                    .andExpect(status().isBadRequest());

            verify(transaccionService, never()).registrarIngreso(any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/transacciones/transferencias")
    class TransferenciasWebTests {

        @Test
        void registrarTransferencia_Valida_Retorna201() throws Exception {
            CrearTransferenciaDTO dto = CrearTransferenciaDTO.builder()
                    .montoCentavos(20_000L)
                    .descripcion("Extracción")
                    .cuentaOrigenId(10L)
                    .cuentaDestinoId(20L)
                    .metodoPagoId(2L)
                    .build();

            Transferencia tx = Transferencia.builder()
                    .id(102L).montoCentavos(20_000L).descripcion("Extracción")
                    .fecha(LocalDateTime.now()).cuentaOrigen(cuentaOrigen).cuentaDestino(cuentaDestino).build();
            when(transaccionService.registrarTransferencia(any(CrearTransferenciaDTO.class), eq(1L)))
                    .thenReturn(TransaccionResponseDTO.fromEntity(tx));

            mockMvc.perform(post("/api/transacciones/transferencias")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipoMovimiento").value("TRANSFERENCIA"))
                    .andExpect(jsonPath("$.cuentaDestinoId").value(20L));
        }

        @Test
        void registrarTransferencia_MismaCuentaOrigenYDestino_Retorna400() throws Exception {
            CrearTransferenciaDTO dto = CrearTransferenciaDTO.builder()
                    .montoCentavos(20_000L)
                    .cuentaOrigenId(10L)
                    .cuentaDestinoId(10L)
                    .metodoPagoId(2L)
                    .build();

            mockMvc.perform(post("/api/transacciones/transferencias")
                            .with(authentication(authUsuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erroresCampos.cuentasDistintas").exists());

            verify(transaccionService, never()).registrarTransferencia(any(), any());
        }
    }

    @Nested
    @DisplayName("PATCH /api/transacciones/{id}/anular")
    class AnularTransaccionWebTests {

        @Test
        void anularTransaccion_PropietarioValido_Retorna200ConEstadoAnulado() throws Exception {
            // Partición válida: transacción activa del usuario autenticado
            Gasto anulado = Gasto.builder()
                    .id(100L).montoCentavos(5_000L).descripcion("Almuerzo")
                    .estadoNegocio(EstadoTransaccion.ANULADO)
                    .fecha(LocalDateTime.now()).cuentaOrigen(cuentaOrigen).build();

            when(transaccionService.anularTransaccion(100L, 1L))
                    .thenReturn(TransaccionResponseDTO.fromEntity(anulado));

            mockMvc.perform(patch("/api/transacciones/{id}/anular", 100L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100L))
                    .andExpect(jsonPath("$.estadoNegocio").value("ANULADO"));
        }

        @Test
        void anularTransaccion_YaAnulada_Retorna409() throws Exception {
            // Partición inválida: conflicto de estado previo
            when(transaccionService.anularTransaccion(100L, 1L))
                    .thenThrow(new IllegalStateException("La transacción ya se encuentra anulada."));

            mockMvc.perform(patch("/api/transacciones/{id}/anular", 100L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensaje").value("La transacción ya se encuentra anulada."));
        }

        @Test
        void anularTransaccion_Inexistente_Retorna404() throws Exception {
            // Partición inválida: ID inexistente en base de datos
            when(transaccionService.anularTransaccion(999L, 1L))
                    .thenThrow(new EntityNotFoundException("Transacción no encontrada con ID: 999"));

            mockMvc.perform(patch("/api/transacciones/{id}/anular", 999L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.mensaje").value("Transacción no encontrada con ID: 999"));
        }

        @Test
        void anularTransaccion_DeOtroUsuario_Retorna403() throws Exception {
            // Partición inválida: intento de anular recurso ajeno
            when(transaccionService.anularTransaccion(99L, 1L))
                    .thenThrow(new AccessDeniedException("No tenés permiso para anular esta transacción."));

            mockMvc.perform(patch("/api/transacciones/{id}/anular", 99L)
                            .with(authentication(authUsuario)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void anularTransaccion_SinAutenticacion_Retorna403() throws Exception {
            // Partición inválida: petición anónima
            mockMvc.perform(patch("/api/transacciones/{id}/anular", 100L))
                    .andExpect(status().isForbidden());
        }
    }
}