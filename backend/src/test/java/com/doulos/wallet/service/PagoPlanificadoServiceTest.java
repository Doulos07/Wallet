package com.doulos.wallet.service;

import com.doulos.wallet.dto.pagoPlanificado.CrearPagoPorEventoDTO;
import com.doulos.wallet.dto.pagoPlanificado.PagoPlanificadoResponseDTO;
import com.doulos.wallet.dto.transaccion.CrearGastoDTO;
import com.doulos.wallet.entity.*;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.repository.CategoriaRepository;
import com.doulos.wallet.repository.MetodoPagoRepository;
import com.doulos.wallet.repository.PagoPlanificadoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoPlanificadoServiceTest {

    @Mock
    private PagoPlanificadoRepository pagoPlanificadoRepository;

    @Mock
    private CuentaService cuentaService;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    @InjectMocks
    private PagoPlanificadoService pagoPlanificadoService;

    private Usuario usuario;
    private Cuenta cuentaOrigen;
    private Cuenta cuentaExterna;
    private Categoria categoria;
    private MetodoPago metodoPago;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).nombre("Santiago").build();
        cuentaOrigen = Cuenta.builder().id(10L).nombre("Banco Galicia").tipo(TipoCuenta.CUENTA_GENERAL).usuario(usuario).build();
        cuentaExterna = Cuenta.builder().id(99L).nombre("__CUENTA_EXTERNA__").tipo(TipoCuenta.EXTERNA).usuario(usuario).build();
        categoria = Categoria.builder().id(5L).nombre("Servicios").build();
        metodoPago = MetodoPago.builder().id(2L).nombre("Débito").build();
    }

    @Nested
    @DisplayName("Creación y Listado de Pagos Planificados")
    class CrearYListarTests {

        @Test
        void crear_DeberiaGuardarPagoYPlantillaSinAsiento() {
            LocalDate fechaInicio = LocalDate.of(2026, 12, 1);
            CrearGastoDTO plantillaDto = CrearGastoDTO.builder()
                    .montoCentavos(150_000L)
                    .descripcion("Internet Fibra")
                    .tercero("Personal")
                    .cuentaOrigenId(10L)
                    .categoriaId(5L)
                    .metodoPagoId(2L)
                    .build();
            CrearPagoPorEventoDTO dto = new CrearPagoPorEventoDTO(fechaInicio, plantillaDto, ChronoUnit.MONTHS, 3);

            when(cuentaService.obtenerCuentaValida(10L, 1L)).thenReturn(cuentaOrigen);
            when(cuentaService.obtenerCuentaExterna(1L)).thenReturn(cuentaExterna);
            when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria));
            when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(metodoPago));
            when(pagoPlanificadoRepository.save(any(PagoPlanificado.class))).thenAnswer(inv -> {
                PagoPlanificado p = inv.getArgument(0);
                p.setId(100L);
                return p;
            });

            PagoPlanificadoResponseDTO response = pagoPlanificadoService.crear(dto, 1L);

            assertNotNull(response);
            assertEquals(100L, response.id());
            assertTrue(response.activo());
            assertEquals(150_000L, response.transaccion().montoCentavos());
            verify(pagoPlanificadoRepository).save(argThat(pago ->
                    pago.obtenerPlantilla() != null && pago.obtenerPlantilla().getAsiento() == null
            ));
        }

        @Test
        void crear_CuandoCategoriaOMetodoNoExisten_DeberiaLanzarEntityNotFound() {
            CrearGastoDTO plantillaDto = CrearGastoDTO.builder()
                    .montoCentavos(50_000L).cuentaOrigenId(10L).categoriaId(404L).metodoPagoId(2L).build();
            CrearPagoPorEventoDTO dto = new CrearPagoPorEventoDTO(LocalDate.now(), plantillaDto, ChronoUnit.MONTHS, 2);

            when(cuentaService.obtenerCuentaValida(10L, 1L)).thenReturn(cuentaOrigen);
            when(cuentaService.obtenerCuentaExterna(1L)).thenReturn(cuentaExterna);
            when(categoriaRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> pagoPlanificadoService.crear(dto, 1L));
            verify(pagoPlanificadoRepository, never()).save(any());
        }

        @Test
        void listarActivos_ConYSinResultados_DeberiaMapearCorrectamente() {
            PagoPlanificado activo = crearPagoUnicoConPlantilla(LocalDate.of(2026, 12, 5));
            activo.setId(1L);

            when(pagoPlanificadoRepository.findActivosConDetalleByUsuarioId(1L)).thenReturn(List.of(activo));
            assertEquals(1, pagoPlanificadoService.listarActivos(1L).size());

            // Frontera lista vacía
            when(pagoPlanificadoRepository.findActivosConDetalleByUsuarioId(2L)).thenReturn(Collections.emptyList());
            assertTrue(pagoPlanificadoService.listarActivos(2L).isEmpty());
        }
    }

    @Nested
    @DisplayName("Desactivación Manual")
    class DesactivarTests {

        @Test
        void desactivar_CuandoEsPropietario_DeberiaMarcarInactivo() {
            PagoPlanificado pago = crearPagoUnicoConPlantilla(LocalDate.of(2026, 11, 20));
            when(pagoPlanificadoRepository.findById(50L)).thenReturn(Optional.of(pago));

            pagoPlanificadoService.desactivar(50L, 1L);

            assertFalse(pago.isActivo());
        }

        @Test
        void desactivar_CuandoUsuarioNoEsPropietario_DeberiaLanzarAccessDenied() {
            PagoPlanificado pago = crearPagoUnicoConPlantilla(LocalDate.of(2026, 11, 20));
            when(pagoPlanificadoRepository.findById(50L)).thenReturn(Optional.of(pago));

            assertThrows(AccessDeniedException.class, () -> pagoPlanificadoService.desactivar(50L, 999L));
            assertTrue(pago.isActivo());
        }

        @Test
        void desactivar_CuandoNoExiste_DeberiaLanzarEntityNotFound() {
            when(pagoPlanificadoRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> pagoPlanificadoService.desactivar(404L, 1L));
        }
    }

    @Nested
    @DisplayName("Ejecución Periódica (Particiones de Herencia y Fronteras de Corte)")
    class EjecutarPendientesTests {

        @Test
        void ejecutarPendientes_FronteraSinPagosVencidos_RetornaCero() {
            LocalDate hoy = LocalDate.of(2026, 11, 20);
            when(pagoPlanificadoRepository.findEjecutablesHasta(hoy)).thenReturn(Collections.emptyList());

            assertEquals(0, pagoPlanificadoService.ejecutarPendientes(hoy));
        }

        @Test
        void ejecutarPendientes_DeberiaClonarPlantillaGenerarAsientoYActualizarCadaTipo() {
            LocalDate hoy = LocalDate.of(2026, 11, 20);

            // 1. Pago único: se desactiva tras ejecutarse
            PagoPlanificado unico = crearPagoUnicoConPlantilla(hoy);

            // 2. Pago infinito: avanza 1 mes y sigue activo
            PagoInfinito infinito = PagoInfinito.builder()
                    .fecha(hoy).activo(true).frecuencia(ChronoUnit.MONTHS).build();
            infinito.agregarTransaccion(crearGastoPlantilla());

            // 3. Pago hasta fecha (supera fechaFin): se desactiva
            PagoHastaFecha hastaFechaVencido = PagoHastaFecha.builder()
                    .fecha(hoy).activo(true).frecuencia(ChronoUnit.MONTHS).fechaFin(LocalDate.of(2026, 12, 10)).build();
            hastaFechaVencido.agregarTransaccion(crearGastoPlantilla());

            // 4. Pago por evento en última cuota (2 de 2): se desactiva
            PagoPorEvento porEventoFinal = PagoPorEvento.builder()
                    .fecha(hoy).activo(true).frecuencia(ChronoUnit.WEEKS).cantidadEventos(2).eventoActual(2).build();
            porEventoFinal.agregarTransaccion(crearGastoPlantilla());

            when(pagoPlanificadoRepository.findEjecutablesHasta(hoy))
                    .thenReturn(List.of(unico, infinito, hastaFechaVencido, porEventoFinal));

            int ejecutados = pagoPlanificadoService.ejecutarPendientes(hoy);

            assertEquals(4, ejecutados);

            assertFalse(unico.isActivo());
            assertEquals(2, unico.getTransaccionesGeneradas().size());
            Transaccion ejecutadaUnico = unico.getTransaccionesGeneradas().get(1);
            assertNotNull(ejecutadaUnico.getAsiento());
            assertEquals(2, ejecutadaUnico.getAsiento().getLineas().size());

            assertTrue(infinito.isActivo());
            assertEquals(LocalDate.of(2026, 12, 20), infinito.getFecha());

            assertFalse(hastaFechaVencido.isActivo());
            assertEquals(LocalDate.of(2026, 12, 20), hastaFechaVencido.getFecha());

            assertFalse(porEventoFinal.isActivo());
            assertEquals(3, porEventoFinal.getEventoActual());
        }

        @Test
        void ejecutarPendientes_FronterasIntermediasDeHastaFechaYPorEvento_DebenSeguirActivos() {
            LocalDate hoy = LocalDate.of(2026, 11, 20);

            // Frontera exacta: al avanzar 1 mes cae exactamente en fechaFin (2026-12-20) -> sigue activo
            PagoHastaFecha hastaFechaExacto = PagoHastaFecha.builder()
                    .fecha(hoy).activo(true).frecuencia(ChronoUnit.MONTHS).fechaFin(LocalDate.of(2026, 12, 20)).build();
            hastaFechaExacto.agregarTransaccion(crearGastoPlantilla());

            // Frontera cuota intermedia: ejecuta cuota 1 de 3 -> pasa a cuota 2 y sigue activo
            PagoPorEvento porEventoIntermedio = PagoPorEvento.builder()
                    .fecha(hoy).activo(true).frecuencia(ChronoUnit.MONTHS).cantidadEventos(3).eventoActual(1).build();
            porEventoIntermedio.agregarTransaccion(crearGastoPlantilla());

            // Frontera cuota única: ejecuta cuota 1 de 1 -> pasa a 2 y se desactiva
            PagoPorEvento porEventoUnico = PagoPorEvento.builder()
                    .fecha(hoy).activo(true).frecuencia(ChronoUnit.DAYS).cantidadEventos(1).eventoActual(1).build();
            porEventoUnico.agregarTransaccion(crearGastoPlantilla());

            when(pagoPlanificadoRepository.findEjecutablesHasta(hoy))
                    .thenReturn(List.of(hastaFechaExacto, porEventoIntermedio, porEventoUnico));

            pagoPlanificadoService.ejecutarPendientes(hoy);

            assertTrue(hastaFechaExacto.isActivo());
            assertEquals(LocalDate.of(2026, 12, 20), hastaFechaExacto.getFecha());

            assertTrue(porEventoIntermedio.isActivo());
            assertEquals(2, porEventoIntermedio.getEventoActual());

            assertFalse(porEventoUnico.isActivo());
            assertEquals(2, porEventoUnico.getEventoActual());
        }
    }

    private PagoPlanificado crearPagoUnicoConPlantilla(LocalDate fecha) {
        PagoPlanificado pago = PagoPlanificado.builder().fecha(fecha).activo(true).build();
        pago.agregarTransaccion(crearGastoPlantilla());
        return pago;
    }

    private Gasto crearGastoPlantilla() {
        return Gasto.builder()
                .montoCentavos(50_000L)
                .descripcion("Suscripción")
                .fecha(LocalDateTime.of(2026, 11, 1, 0, 0))
                .cuentaOrigen(cuentaOrigen)
                .cuentaDestino(cuentaExterna)
                .categoria(categoria)
                .metodoPago(metodoPago)
                .build();
    }
}