package com.doulos.wallet.service;

import com.doulos.wallet.dto.transaccion.CrearGastoDTO;
import com.doulos.wallet.dto.transaccion.CrearIngresoDTO;
import com.doulos.wallet.dto.transaccion.CrearTransferenciaDTO;
import com.doulos.wallet.dto.transaccion.TransaccionResponseDTO;
import com.doulos.wallet.entity.*;
import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.EstadoTransaccion;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.entity.enums.TipoMovimiento;
import com.doulos.wallet.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransaccionServiceTest {

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    @InjectMocks
    private TransaccionService transaccionService;

    @Mock
    private AsientoRepository asientoRepository;

    private Usuario usuario;
    private Cuenta cuentaOrigen;
    private Cuenta cuentaDestino;
    private Cuenta cuentaExterna;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).nombre("Santiago").build();

        cuentaOrigen = Cuenta.builder()
                .id(10L)
                .nombre("Mercado Pago")
                .tipo(TipoCuenta.CUENTA_GENERAL)
                .saldoCentavos(100_000L)
                .usuario(usuario)
                .build();

        cuentaDestino = Cuenta.builder()
                .id(20L)
                .nombre("Efectivo")
                .tipo(TipoCuenta.CUENTA_AHORRO)
                .saldoCentavos(50_000L)
                .usuario(usuario)
                .build();

        cuentaExterna = Cuenta.crearCuentaExterna(usuario);
        cuentaExterna.setId(999L);
    }


    @Nested
    @DisplayName("Consulta Paginada con Filtros")
    class ListarTransaccionesTests {

        @Test
        void listarTransacciones_ConRangoFechas_DeberiaConvertirALimitesDelDiaYMapearPagina() {
            LocalDate desde = LocalDate.of(2026, 11, 1);
            LocalDate hasta = LocalDate.of(2026, 11, 23);
            Pageable pageable = PageRequest.of(0, 20);

            Gasto gasto = Gasto.builder()
                    .id(100L).montoCentavos(5_000L).descripcion("Café")
                    .fecha(LocalDateTime.of(2026, 11, 10, 14, 30)).cuentaOrigen(cuentaOrigen).build();
            Page<Transaccion> paginaEntidades = new PageImpl<>(List.of(gasto), pageable, 1);

            when(transaccionRepository.buscarPorUsuarioConFiltros(
                    eq(1L),
                    eq(10L),
                    eq(EstadoTransaccion.VIGENTE),
                    eq(EstadoConciliacion.LIQUIDADO),
                    eq(desde.atStartOfDay()),
                    eq(hasta.atTime(LocalTime.MAX)),
                    eq(pageable)
            )).thenReturn(paginaEntidades);

            Page<TransaccionResponseDTO> resultado = transaccionService.listarTransacciones(
                    1L, 10L, EstadoTransaccion.VIGENTE, EstadoConciliacion.LIQUIDADO, desde, hasta, pageable);

            assertEquals(1, resultado.getTotalElements());
            assertEquals(100L, resultado.getContent().get(0).id());
            assertEquals("GASTO", resultado.getContent().get(0).tipoMovimiento());
        }

        @Test
        void listarTransacciones_ConFechasNulas_DeberiaEnviarNullAlRepositorio() {
            Pageable pageable = PageRequest.of(0, 20);
            when(transaccionRepository.buscarPorUsuarioConFiltros(1L, null, null, null, null, null, pageable))
                    .thenReturn(Page.empty(pageable));

            Page<TransaccionResponseDTO> resultado = transaccionService.listarTransacciones(
                    1L, null, null, null, null, null, pageable);

            assertTrue(resultado.isEmpty());
            verify(transaccionRepository).buscarPorUsuarioConFiltros(1L, null, null, null, null, null, pageable);
        }
    }




    @Nested
    @DisplayName("Registro de Ingresos")
    class IngresosTests {

        @Test
        void registrarIngreso_DeberiaAcreditarSaldoYGenerarAsientoBalanceado() {
            CrearIngresoDTO dto = CrearIngresoDTO.builder()
                    .montoCentavos(50_000L)
                    .descripcion("Cobro freelance")
                    .tercero("Cliente X")
                    .cuentaOrigenId(10L)
                    .build();

            when(cuentaRepository.findById(10L)).thenReturn(Optional.of(cuentaOrigen));
            when(cuentaRepository.findByUsuarioIdAndTipo(1L, TipoCuenta.EXTERNA)).thenReturn(Optional.of(cuentaExterna));
            simularGuardado();

            TransaccionResponseDTO response = transaccionService.registrarIngreso(dto, 1L);

            assertEquals(150_000L, cuentaOrigen.getSaldoCentavos());
            assertEquals(-50_000L, cuentaExterna.getSaldoCentavos());
            assertEquals("INGRESO", response.tipoMovimiento());
            assertEquals(50_000L, response.montoCentavos());
            assertNull(response.cuentaDestinoId());
            assertNull(response.cuentaDestinoNombre());

            Transaccion guardada = capturarTransaccionGuardada();
            assertInstanceOf(Ingreso.class, guardada);
            assertEquals(2, guardada.getAsiento().getLineas().size());
            assertEquals(TipoMovimiento.DEBITO, guardada.getAsiento().getLineas().get(0).getTipoMovimiento());
            assertEquals(TipoMovimiento.CREDITO, guardada.getAsiento().getLineas().get(1).getTipoMovimiento());
        }

        @Test
        void registrarIngresoInicial_DeberiaAcreditarCuentaNuevaYDebitarExterna() {
            Cuenta cuentaNueva = Cuenta.builder()
                    .id(30L).nombre("Caja de Ahorro").tipo(TipoCuenta.CUENTA_AHORRO).saldoCentavos(0L).usuario(usuario).build();

            when(cuentaRepository.findByUsuarioIdAndTipo(1L, TipoCuenta.EXTERNA)).thenReturn(Optional.of(cuentaExterna));
            simularGuardado();

            transaccionService.registrarIngresoInicial(cuentaNueva, 200_000L, 1L);

            assertEquals(200_000L, cuentaNueva.getSaldoCentavos());
            assertEquals(-200_000L, cuentaExterna.getSaldoCentavos());
            Transaccion guardada = capturarTransaccionGuardada();
            assertEquals("Saldo inicial", guardada.getDescripcion());
        }

        @Test
        void registrarIngreso_SobreCuentaExternaOAjena_DeberiaRechazar() {
            CrearIngresoDTO dtoExterna = CrearIngresoDTO.builder().montoCentavos(10_000L).cuentaOrigenId(999L).build();
            when(cuentaRepository.findById(999L)).thenReturn(Optional.of(cuentaExterna));
            assertThrows(IllegalArgumentException.class, () -> transaccionService.registrarIngreso(dtoExterna, 1L));

            Cuenta ajena = crearCuentaAjena(30L);
            CrearIngresoDTO dtoAjena = CrearIngresoDTO.builder().montoCentavos(10_000L).cuentaOrigenId(30L).build();
            when(cuentaRepository.findById(30L)).thenReturn(Optional.of(ajena));
            assertThrows(AccessDeniedException.class, () -> transaccionService.registrarIngreso(dtoAjena, 1L));
        }
    }

    @Nested
    @DisplayName("Registro de Gastos (Particiones y Fronteras de Saldo)")
    class GastosTests {

        @Test
        void registrarGasto_ConCategoriaYMetodoPago_DeberiaDebitarSaldoYGenerarAsiento() {
            CrearGastoDTO dto = CrearGastoDTO.builder()
                    .montoCentavos(30_000L)
                    .descripcion("Supermercado")
                    .tercero("Carrefour")
                    .garantiaMeses(0)
                    .cuentaOrigenId(10L)
                    .categoriaId(5L)
                    .metodoPagoId(2L)
                    .build();

            when(cuentaRepository.findById(10L)).thenReturn(Optional.of(cuentaOrigen));
            when(cuentaRepository.findByUsuarioIdAndTipo(1L, TipoCuenta.EXTERNA)).thenReturn(Optional.of(cuentaExterna));
            when(categoriaRepository.getReferenceById(5L)).thenReturn(Categoria.builder().id(5L).build());
            when(metodoPagoRepository.getReferenceById(2L)).thenReturn(MetodoPago.builder().id(2L).build());
            simularGuardado();

            TransaccionResponseDTO response = transaccionService.registrarGasto(dto, 1L);

            assertEquals(70_000L, cuentaOrigen.getSaldoCentavos());
            assertEquals(30_000L, cuentaExterna.getSaldoCentavos());
            assertEquals("GASTO", response.tipoMovimiento());
            assertEquals(5L, response.categoriaId());
            assertEquals(2L, response.metodoPagoId());
            assertNull(response.cuentaDestinoId());
            assertNull(response.cuentaDestinoNombre());

            Transaccion guardada = capturarTransaccionGuardada();
            assertInstanceOf(Gasto.class, guardada);
            assertEquals(TipoMovimiento.DEBITO, guardada.getAsiento().getLineas().get(0).getTipoMovimiento());
            assertEquals(TipoMovimiento.CREDITO, guardada.getAsiento().getLineas().get(1).getTipoMovimiento());
        }

        @Test
        void registrarGasto_FronteraSaldoExactoEnCeroYSinOpcionales_DeberiaDejarCuentaEnCero() {
            // Gastar exactamente el 100% del saldo disponible (100.000 centavos) sin categoría ni método
            CrearGastoDTO dto = CrearGastoDTO.builder()
                    .montoCentavos(100_000L)
                    .descripcion("Pago total")
                    .cuentaOrigenId(10L)
                    .categoriaId(null)
                    .metodoPagoId(null)
                    .build();

            when(cuentaRepository.findById(10L)).thenReturn(Optional.of(cuentaOrigen));
            when(cuentaRepository.findByUsuarioIdAndTipo(1L, TipoCuenta.EXTERNA)).thenReturn(Optional.of(cuentaExterna));
            simularGuardado();

            TransaccionResponseDTO response = transaccionService.registrarGasto(dto, 1L);

            assertEquals(0L, cuentaOrigen.getSaldoCentavos());
            assertNull(response.categoriaId());
            assertNull(response.metodoPagoId());
            verify(categoriaRepository, never()).getReferenceById(any());
            verify(metodoPagoRepository, never()).getReferenceById(any());
        }

        @Test
        void registrarGasto_SobreCuentaExternaAjenaOInexistente_DeberiaRechazar() {
            CrearGastoDTO dtoExterna = CrearGastoDTO.builder().montoCentavos(10_000L).cuentaOrigenId(999L).build();
            when(cuentaRepository.findById(999L)).thenReturn(Optional.of(cuentaExterna));
            assertThrows(IllegalArgumentException.class, () -> transaccionService.registrarGasto(dtoExterna, 1L));

            Cuenta cuentaAjena = crearCuentaAjena(30L);
            CrearGastoDTO dtoAjena = CrearGastoDTO.builder().montoCentavos(10_000L).cuentaOrigenId(30L).build();
            when(cuentaRepository.findById(30L)).thenReturn(Optional.of(cuentaAjena));
            assertThrows(AccessDeniedException.class, () -> transaccionService.registrarGasto(dtoAjena, 1L));
            assertEquals(100_000L, cuentaAjena.getSaldoCentavos());

            CrearGastoDTO dtoInexistente = CrearGastoDTO.builder().montoCentavos(10_000L).cuentaOrigenId(404L).build();
            when(cuentaRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> transaccionService.registrarGasto(dtoInexistente, 1L));

            verify(transaccionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Registro de Transferencias")
    class TransferenciasTests {

        @Test
        void registrarTransferencia_DeberiaMoverSaldoEntreCuentas() {
            CrearTransferenciaDTO dto = CrearTransferenciaDTO.builder()
                    .montoCentavos(20_000L)
                    .descripcion("Extracción a efectivo")
                    .cuentaOrigenId(10L)
                    .cuentaDestinoId(20L)
                    .build();

            when(cuentaRepository.findById(10L)).thenReturn(Optional.of(cuentaOrigen));
            when(cuentaRepository.findById(20L)).thenReturn(Optional.of(cuentaDestino));
            simularGuardado();

            TransaccionResponseDTO response = transaccionService.registrarTransferencia(dto, 1L);

            assertEquals(80_000L, cuentaOrigen.getSaldoCentavos());
            assertEquals(70_000L, cuentaDestino.getSaldoCentavos());
            assertEquals("TRANSFERENCIA", response.tipoMovimiento());
            assertEquals(20L, response.cuentaDestinoId());
            assertEquals("Efectivo", response.cuentaDestinoNombre());

            Transaccion guardada = capturarTransaccionGuardada();
            assertInstanceOf(Transferencia.class, guardada);
            assertEquals(2, guardada.getAsiento().getLineas().size());
        }

        @Test
        void registrarTransferencia_MismaCuentaOCuentaExterna_DeberiaLanzarExcepcion() {
            // Frontera: origen == destino
            CrearTransferenciaDTO dtoMisma = CrearTransferenciaDTO.builder()
                    .montoCentavos(10_000L).cuentaOrigenId(10L).cuentaDestinoId(10L).build();
            assertThrows(IllegalArgumentException.class, () -> transaccionService.registrarTransferencia(dtoMisma, 1L));

            // Partición inválida: destino es cuenta externa
            CrearTransferenciaDTO dtoDestinoExterno = CrearTransferenciaDTO.builder()
                    .montoCentavos(10_000L).cuentaOrigenId(10L).cuentaDestinoId(999L).build();
            when(cuentaRepository.findById(10L)).thenReturn(Optional.of(cuentaOrigen));
            when(cuentaRepository.findById(999L)).thenReturn(Optional.of(cuentaExterna));
            assertThrows(IllegalArgumentException.class, () -> transaccionService.registrarTransferencia(dtoDestinoExterno, 1L));

            verify(transaccionRepository, never()).save(any());
        }

        @Test
        void registrarTransferencia_CuentaOrigenODestinoAjena_DeberiaLanzarAccessDenied() {
            Cuenta ajena = crearCuentaAjena(30L);

            CrearTransferenciaDTO dtoDestinoAjeno = CrearTransferenciaDTO.builder()
                    .montoCentavos(10_000L).cuentaOrigenId(10L).cuentaDestinoId(30L).build();
            when(cuentaRepository.findById(10L)).thenReturn(Optional.of(cuentaOrigen));
            when(cuentaRepository.findById(30L)).thenReturn(Optional.of(ajena));

            assertThrows(AccessDeniedException.class, () -> transaccionService.registrarTransferencia(dtoDestinoAjeno, 1L));
            verify(transaccionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Anulación de Transacciones y Contra-asientos")
    class AnularTransaccionTests {

        @Test
        void anularTransaccion_DeGastoActivo_DeberiaReintegrarSaldoYGenerarContraAsiento() {
            Asiento asientoOriginal = crearAsientoPrueba(cuentaOrigen, cuentaExterna, 30_000L);
            Gasto gasto = Gasto.builder()
                    .id(100L)
                    .montoCentavos(30_000L)
                    .estadoNegocio(EstadoTransaccion.VIGENTE)
                    .cuentaOrigen(cuentaOrigen)
                    .cuentaDestino(cuentaExterna)
                    .asiento(asientoOriginal)
                    .build();

            when(transaccionRepository.findById(100L)).thenReturn(Optional.of(gasto));
            simularGuardado();

            TransaccionResponseDTO resultado = transaccionService.anularTransaccion(100L, 1L);

            assertEquals(EstadoTransaccion.ANULADO, resultado.estadoNegocio());
            assertEquals(130_000L, cuentaOrigen.getSaldoCentavos());

            ArgumentCaptor<Asiento> asientoCaptor = ArgumentCaptor.forClass(Asiento.class);
            verify(asientoRepository).save(asientoCaptor.capture());

            Asiento contraAsiento = asientoCaptor.getValue();
            assertEquals(2, contraAsiento.getLineas().size());
            assertEquals(TipoMovimiento.CREDITO, contraAsiento.getLineas().get(0).getTipoMovimiento());
            assertEquals(TipoMovimiento.DEBITO, contraAsiento.getLineas().get(1).getTipoMovimiento());
        }

        @Test
        void anularTransaccion_DeIngresoYTransferencia_DeberiaRevertirSaldosCorrespondientes() {
            // Caso 1: Ingreso se revierte descontando saldo
            Ingreso ingreso = Ingreso.builder()
                    .id(101L).montoCentavos(40_000L).estadoNegocio(EstadoTransaccion.VIGENTE)
                    .cuentaOrigen(cuentaOrigen).build();
            when(transaccionRepository.findById(101L)).thenReturn(Optional.of(ingreso));
            simularGuardado();

            transaccionService.anularTransaccion(101L, 1L);
            assertEquals(60_000L, cuentaOrigen.getSaldoCentavos());

            // Caso 2: Transferencia devuelve a origen y descuenta de destino
            Transferencia transferencia = Transferencia.builder()
                    .id(102L).montoCentavos(20_000L).estadoNegocio(EstadoTransaccion.VIGENTE)
                    .cuentaOrigen(cuentaOrigen).cuentaDestino(cuentaDestino).build();
            when(transaccionRepository.findById(102L)).thenReturn(Optional.of(transferencia));

            transaccionService.anularTransaccion(102L, 1L);
            assertEquals(80_000L, cuentaOrigen.getSaldoCentavos());
            assertEquals(30_000L, cuentaDestino.getSaldoCentavos());
        }

        @Test
        void anularTransaccion_YaAnuladaAjenaOInexistente_DeberiaRechazar() {
            // Frontera: transacción ya anulada (409)
            Gasto yaAnulada = Gasto.builder()
                    .id(100L).montoCentavos(10_000L).estadoNegocio(EstadoTransaccion.ANULADO)
                    .cuentaOrigen(cuentaOrigen).build();
            when(transaccionRepository.findById(100L)).thenReturn(Optional.of(yaAnulada));
            assertThrows(IllegalStateException.class, () -> transaccionService.anularTransaccion(100L, 1L));

            // Partición inválida: cuenta de otro usuario (403)
            Cuenta ajena = crearCuentaAjena(30L);
            Gasto gastoAjeno = Gasto.builder()
                    .id(101L).montoCentavos(10_000L).estadoNegocio(EstadoTransaccion.VIGENTE)
                    .cuentaOrigen(ajena).build();
            when(transaccionRepository.findById(101L)).thenReturn(Optional.of(gastoAjeno));
            assertThrows(AccessDeniedException.class, () -> transaccionService.anularTransaccion(101L, 1L));

            // Partición inválida: ID inexistente (404)
            when(transaccionRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> transaccionService.anularTransaccion(404L, 1L));

            verify(asientoRepository, never()).save(any());
            verify(transaccionRepository, never()).save(any());
        }

        private Asiento crearAsientoPrueba(Cuenta debito, Cuenta credito, Long monto) {
            Asiento asiento = Asiento.builder().fecha(LocalDateTime.now()).build();
            asiento.getLineas().add(LineaAsiento.builder()
                    .cuenta(debito).montoCentavos(monto).tipoMovimiento(TipoMovimiento.DEBITO).asiento(asiento).build());
            asiento.getLineas().add(LineaAsiento.builder()
                    .cuenta(credito).montoCentavos(monto).tipoMovimiento(TipoMovimiento.CREDITO).asiento(asiento).build());
            return asiento;
        }
    }

    private Cuenta crearCuentaAjena(Long id) {
        Usuario otroUsuario = Usuario.builder().id(99L).build();
        return Cuenta.builder()
                .id(id).tipo(TipoCuenta.EFECTIVO).saldoCentavos(100_000L).usuario(otroUsuario).build();
    }

    private void simularGuardado() {
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocation -> {
            Transaccion tx = invocation.getArgument(0);
            tx.setId(100L);
            return tx;
        });
    }

    private Transaccion capturarTransaccionGuardada() {
        ArgumentCaptor<Transaccion> captor = ArgumentCaptor.forClass(Transaccion.class);
        verify(transaccionRepository).save(captor.capture());
        return captor.getValue();
    }


}