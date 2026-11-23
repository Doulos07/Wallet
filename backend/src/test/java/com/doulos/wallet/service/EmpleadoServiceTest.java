package com.doulos.wallet.service;

import com.doulos.wallet.dto.empleado.CrearAdelantoSueldoDTO;
import com.doulos.wallet.dto.empleado.CrearEmpleadoDTO;
import com.doulos.wallet.dto.empleado.CrearRegistroExtraDTO;
import com.doulos.wallet.dto.empleado.EmpleadoResponseDTO;
import com.doulos.wallet.entity.*;
import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.entity.enums.TipoPagoHoraExtra;
import com.doulos.wallet.repository.CategoriaRepository;
import com.doulos.wallet.repository.EmpleadoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceTest {

    @Mock
    private EmpleadoRepository empleadoRepository;

    @Mock
    private PagoPlanificadoRepository pagoPlanificadoRepository;

    @Mock
    private CuentaService cuentaService;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private MetodoPagoRepository metodoPagoRepository;

    @InjectMocks
    private EmpleadoService empleadoService;

    private Usuario jefe;
    private Cuenta cuentaOrigen;
    private Cuenta cuentaExterna;
    private Categoria categoria;
    private MetodoPago metodoPago;

    @BeforeEach
    void setUp() {
        jefe = Usuario.builder().id(1L).nombre("Santiago").build();
        cuentaOrigen = Cuenta.builder().id(10L).nombre("Banco Galicia").tipo(TipoCuenta.CUENTA_GENERAL).usuario(jefe).build();
        cuentaExterna = Cuenta.builder().id(99L).nombre("__CUENTA_EXTERNA__").tipo(TipoCuenta.EXTERNA).usuario(jefe).build();
        categoria = Categoria.builder().id(5L).nombre("Sueldos").build();
        metodoPago = MetodoPago.builder().id(2L).nombre("Transferencia").build();
    }

    @Nested
    @DisplayName("Cálculo de Horas Extra (Particiones y Fronteras)")
    class CalculoHorasExtraTests {

        @Test
        void multiplicadorHora_DeberiaCalcularConPrecisionYRedondeoHalfUp() {
            Empleado emp = crearEmpleadoBase(90_000_000L, 144, new BigDecimal("1.50"), TipoPagoHoraExtra.MULTIPLICADOR_HORA);

            // 625.000 centavos/hora * 1.5 * 2 horas = 1.875.000 centavos ($18.750)
            long monto = emp.calcularMontoExtraCentavos(new BigDecimal("2.0"), null);
            assertEquals(1_875_000L, monto);

            // Caso frontera de redondeo HALF_UP: 100 / 3 * 1 * 1 = 33.33 -> 33; 200 / 3 = 66.66 -> 67
            Empleado empPer = crearEmpleadoBase(200L, 3, BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA);
            assertEquals(67L, empPer.calcularMontoExtraCentavos(BigDecimal.ONE, null));
        }

        @Test
        void montoFijoHora_DeberiaMultiplicarTarifaPorCantidadHoras() {
            Empleado emp = crearEmpleadoBase(90_000_000L, 144, new BigDecimal("800000"), TipoPagoHoraExtra.MONTO_FIJO_HORA);

            long monto = emp.calcularMontoExtraCentavos(new BigDecimal("2.5"), null);
            assertEquals(2_000_000L, monto);
        }

        @Test
        void porTrabajoRealizado_DeberiaTomarMontoManualYFallarEnFronteraCero() {
            Empleado emp = crearEmpleadoBase(90_000_000L, 144, BigDecimal.ONE, TipoPagoHoraExtra.POR_TRABAJO_REALIZADO);

            // Frontera mínima válida (1 centavo)
            assertEquals(1L, emp.calcularMontoExtraCentavos(null, 1L));

            // Fronteras inválidas (0, negativo y null)
            assertThrows(IllegalArgumentException.class, () -> emp.calcularMontoExtraCentavos(null, 0L));
            assertThrows(IllegalArgumentException.class, () -> emp.calcularMontoExtraCentavos(null, -100L));
            assertThrows(IllegalArgumentException.class, () -> emp.calcularMontoExtraCentavos(null, null));
        }

        @Test
        void horasInvalidasOBaseCero_DeberiaLanzarExcepcion() {
            Empleado emp = crearEmpleadoBase(90_000_000L, 144, BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA);

            // Frontera cero y negativa en cantidad de horas
            assertThrows(IllegalArgumentException.class, () -> emp.calcularMontoExtraCentavos(BigDecimal.ZERO, null));
            assertThrows(IllegalArgumentException.class, () -> emp.calcularMontoExtraCentavos(new BigDecimal("-0.01"), null));
            assertThrows(IllegalArgumentException.class, () -> emp.calcularMontoExtraCentavos(null, null));

            // Frontera cero en horas mensuales base
            Empleado empSinHorasBase = crearEmpleadoBase(90_000_000L, 0, BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA);
            assertThrows(IllegalArgumentException.class, () -> empSinHorasBase.calcularMontoExtraCentavos(BigDecimal.ONE, null));
        }
    }

    @Nested
    @DisplayName("Alta de Empleado")
    class CrearEmpleadoTests {

        @Test
        void crear_ConDiasPorDefecto_DeberiaProgramarSueldoElDia10SinAsiento() {
            CrearEmpleadoDTO dto = new CrearEmpleadoDTO(
                    "Juan", "Pérez", "35111222", 90_000_000L, 144,
                    BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA,
                    null, null, 10L, 5L, 2L
            );

            mockearDependenciasCreacion();
            when(empleadoRepository.save(any(Empleado.class))).thenAnswer(inv -> {
                Empleado e = inv.getArgument(0);
                e.setId(7L);
                return e;
            });

            EmpleadoResponseDTO response = empleadoService.crear(dto, jefe);

            assertEquals(7L, response.id());
            assertTrue(response.activo());
            assertEquals(10, response.proximoPagoSueldo().getDayOfMonth());
            verify(empleadoRepository).save(argThat(e ->
                    e.getPagoPlanificadoSueldo() != null
                            && e.getPagoPlanificadoSueldo().obtenerPlantilla().getAsiento() == null
                            && e.getDiaPagoExtras() == 20
            ));
        }

        @Test
        void crear_ConDiaFrontera31_DeberiaAjustarAlUltimoDiaDelMes() {
            CrearEmpleadoDTO dto = new CrearEmpleadoDTO(
                    "Ana", "Gómez", "36222333", 80_000_000L, 160,
                    new BigDecimal("1.5"), TipoPagoHoraExtra.MULTIPLICADOR_HORA,
                    31, 31, 10L, 5L, 2L
            );

            mockearDependenciasCreacion();
            when(empleadoRepository.save(any(Empleado.class))).thenAnswer(inv -> inv.getArgument(0));

            EmpleadoResponseDTO response = empleadoService.crear(dto, jefe);

            int ultimoDiaMesProgramado = response.proximoPagoSueldo().lengthOfMonth();
            assertEquals(ultimoDiaMesProgramado, response.proximoPagoSueldo().getDayOfMonth());
        }
    }

    @Nested
    @DisplayName("Registro y Acumulación de Horas Extra")
    class RegistrarExtraTests {

        @Test
        void registrarExtra_DosVecesEnElPeriodo_DeberiaCrearUnPagoUnicoYAcumularMonto() {
            Empleado emp = crearEmpleadoConSueldoPlanificado();
            when(empleadoRepository.findById(7L)).thenReturn(Optional.of(emp));

            // Primer extra: 2 horas * 625.000 = 1.250.000 centavos
            empleadoService.registrarExtra(7L, new CrearRegistroExtraDTO(
                    LocalDate.of(2026, 11, 5), new BigDecimal("2"), "Guardia sábado", null
            ), 1L);

            PagoPlanificado pagoUnico = emp.obtenerPagoExtrasActivo().orElseThrow();
            assertEquals(1_250_000L, pagoUnico.obtenerPlantilla().getMontoCentavos());
            assertEquals(1, pagoUnico.getRegistrosExtra().size());

            // Segundo extra en el mismo período: 1 hora * 625.000 = 625.000 -> total 1.875.000
            empleadoService.registrarExtra(7L, new CrearRegistroExtraDTO(
                    LocalDate.of(2026, 11, 12), BigDecimal.ONE, "Cierre mensual", null
            ), 1L);

            assertSame(pagoUnico, emp.obtenerPagoExtrasActivo().orElseThrow());
            assertEquals(1_875_000L, pagoUnico.obtenerPlantilla().getMontoCentavos());
            assertEquals(2, pagoUnico.getRegistrosExtra().size());
        }

        @Test
        void registrarExtra_CuandoEmpleadoInactivoOAjenos_DeberiaRechazar() {
            Empleado inactivo = crearEmpleadoConSueldoPlanificado();
            inactivo.setActivo(false);
            when(empleadoRepository.findById(7L)).thenReturn(Optional.of(inactivo));

            CrearRegistroExtraDTO dto = new CrearRegistroExtraDTO(LocalDate.now(), BigDecimal.ONE, "Extra", null);

            // Partición empleado dado de baja
            assertThrows(IllegalStateException.class, () -> empleadoService.registrarExtra(7L, dto, 1L));

            // Partición usuario no propietario
            inactivo.setActivo(true);
            assertThrows(AccessDeniedException.class, () -> empleadoService.registrarExtra(7L, dto, 999L));

            // Partición empleado inexistente
            when(empleadoRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> empleadoService.registrarExtra(404L, dto, 1L));
        }
    }

    @Nested
    @DisplayName("Adelantos de Sueldo y Liquidación de Nómina")
    class LiquidacionNominaTests {

        @Test
        void registrarAdelanto_DeberiaGenerarGastoConAsientoInmediato() {
            Empleado emp = crearEmpleadoConSueldoPlanificado();
            when(empleadoRepository.findById(7L)).thenReturn(Optional.of(emp));
            mockearDependenciasCreacion();

            empleadoService.registrarAdelanto(7L, new CrearAdelantoSueldoDTO(
                    LocalDate.of(2026, 11, 15), 20_000_000L, "Adelanto quincena", 10L, 5L, 2L
            ), 1L);

            assertEquals(1, emp.getAdelantoSueldos().size());
            AdelantoSueldo adelanto = emp.getAdelantoSueldos().getFirst();
            assertFalse(adelanto.isDescontado());
            assertNotNull(adelanto.getPago().getAsiento());
            assertEquals(2, adelanto.getPago().getAsiento().getLineas().size());
        }

        @Test
        void ejecutarSueldo_ConAdelantosParcialesYPrevios_DeberiaDescontarSoloPendientes() {
            Empleado emp = crearEmpleadoConSueldoPlanificado();
            LocalDate diaCobro = emp.getPagoPlanificadoSueldo().getFecha();

            // Adelanto ya descontado anteriormente (no debe volver a restarse)
            emp.agregarAdelantoSueldo(AdelantoSueldo.builder()
                    .fecha(diaCobro.minusMonths(1)).montoCentavos(10_000_000L).descontado(true).build());
            // Adelanto pendiente actual
            AdelantoSueldo pendiente = AdelantoSueldo.builder()
                    .fecha(diaCobro.minusDays(5)).montoCentavos(25_000_000L).descontado(false).build();
            emp.agregarAdelantoSueldo(pendiente);

            emp.getPagoPlanificadoSueldo().ejecutar(diaCobro);

            assertTrue(pendiente.isDescontado());
            // 90.000.000 - 25.000.000 = 65.000.000 centavos
            List<Transaccion> movimientos = emp.getPagoPlanificadoSueldo().getTransaccionesGeneradas();
            assertEquals(2, movimientos.size());
            Transaccion sueldoEjecutado = movimientos.get(1);
            assertEquals(65_000_000L, sueldoEjecutado.getMontoCentavos());
            assertNotNull(sueldoEjecutado.getAsiento());
            assertEquals(diaCobro.plusMonths(1), emp.getPagoPlanificadoSueldo().getFecha());
        }

        @Test
        void ejecutarSueldo_FronteraAdelantosIgualesOMayoresAlSueldo_NoDebeGenerarAsientoEnCero() {
            Empleado emp = crearEmpleadoConSueldoPlanificado();
            LocalDate diaCobro = emp.getPagoPlanificadoSueldo().getFecha();

            // Adelanto igual al 100% del sueldo (90.000.000)
            AdelantoSueldo adelantoTotal = AdelantoSueldo.builder()
                    .fecha(diaCobro.minusDays(2)).montoCentavos(90_000_000L).descontado(false).build();
            emp.agregarAdelantoSueldo(adelantoTotal);

            emp.getPagoPlanificadoSueldo().ejecutar(diaCobro);

            assertTrue(adelantoTotal.isDescontado());
            // Solo permanece la plantilla sin asiento; no se emite movimiento por $0
            assertEquals(1, emp.getPagoPlanificadoSueldo().getTransaccionesGeneradas().size());
            assertTrue(emp.getPagoPlanificadoSueldo().isActivo());
            assertEquals(diaCobro.plusMonths(1), emp.getPagoPlanificadoSueldo().getFecha());
        }

        @Test
        void ejecutarExtras_DeberiaGenerarGastoLiquidarRegistrosYDesactivarPagoUnico() {
            Empleado emp = crearEmpleadoConSueldoPlanificado();
            when(empleadoRepository.findById(7L)).thenReturn(Optional.of(emp));

            empleadoService.registrarExtra(7L, new CrearRegistroExtraDTO(
                    LocalDate.of(2026, 11, 5), new BigDecimal("2"), "Extra 1", null
            ), 1L);

            PagoPlanificado pagoExtras = emp.obtenerPagoExtrasActivo().orElseThrow();
            LocalDate diaPagoExtras = pagoExtras.getFecha();

            pagoExtras.ejecutar(diaPagoExtras);

            assertFalse(pagoExtras.isActivo());
            assertEquals(2, pagoExtras.getTransaccionesGeneradas().size());
            Transaccion gastoExtras = pagoExtras.getTransaccionesGeneradas().get(1);
            assertEquals(1_250_000L, gastoExtras.getMontoCentavos());
            assertNotNull(gastoExtras.getAsiento());

            RegistroExtra registro = emp.getRegistroExtras().getFirst();
            assertEquals(EstadoConciliacion.LIQUIDADO, registro.getEstadoLiquidacion());
            assertSame(gastoExtras, registro.getPago());
        }
    }

    @Nested
    @DisplayName("Listado de Activos y Rechazos en Adelantos")
    class ListadoYValidacionesAdelantoTests {

        @Test
        void listarActivos_ConYSinEmpleados_RetornaListaMapeada() {
            Empleado emp = crearEmpleadoConSueldoPlanificado();
            when(empleadoRepository.findByJefeIdAndActivoTrue(1L)).thenReturn(List.of(emp));

            List<EmpleadoResponseDTO> resultado = empleadoService.listarActivos(1L);

            assertEquals(1, resultado.size());
            assertEquals("Juan", resultado.getFirst().nombre());
            assertEquals("35111222", resultado.getFirst().dni());

            // Frontera lista vacía
            when(empleadoRepository.findByJefeIdAndActivoTrue(1L)).thenReturn(Collections.emptyList());
            assertTrue(empleadoService.listarActivos(1L).isEmpty());
        }

        @Test
        void registrarAdelanto_EmpleadoInactivoOAjeno_DeberiaRechazar() {
            Empleado emp = crearEmpleadoConSueldoPlanificado();
            emp.setActivo(false);
            when(empleadoRepository.findById(7L)).thenReturn(Optional.of(emp));

            CrearAdelantoSueldoDTO dto = new CrearAdelantoSueldoDTO(
                    LocalDate.of(2026, 11, 23), 10_000_000L, "Adelanto", 10L, 5L, 2L
            );

            // Partición empleado dado de baja
            assertThrows(IllegalStateException.class, () -> empleadoService.registrarAdelanto(7L, dto, 1L));

            // Partición empleado de otro jefe
            emp.setActivo(true);
            assertThrows(AccessDeniedException.class, () -> empleadoService.registrarAdelanto(7L, dto, 999L));
        }
    }

    @Nested
    @DisplayName("Baja Lógica de Empleado")
    class DesactivarEmpleadoTests {

        @Test
        void desactivar_DeberiaDetenerEmpleadoSueldoYExtrasActivos() {
            Empleado emp = crearEmpleadoConSueldoPlanificado();
            when(empleadoRepository.findById(7L)).thenReturn(Optional.of(emp));

            empleadoService.registrarExtra(7L, new CrearRegistroExtraDTO(
                    LocalDate.now(), BigDecimal.ONE, "Extra pendiente", null
            ), 1L);
            PagoPlanificado pagoExtras = emp.obtenerPagoExtrasActivo().orElseThrow();

            empleadoService.desactivar(7L, 1L);

            assertFalse(emp.isActivo());
            assertFalse(emp.getPagoPlanificadoSueldo().isActivo());
            assertFalse(pagoExtras.isActivo());
        }
    }

    // ==================== HELPERS ====================

    private void mockearDependenciasCreacion() {
        when(cuentaService.obtenerCuentaValida(10L, 1L)).thenReturn(cuentaOrigen);
        when(cuentaService.obtenerCuentaExterna(1L)).thenReturn(cuentaExterna);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria));
        when(metodoPagoRepository.findById(2L)).thenReturn(Optional.of(metodoPago));
    }

    private Empleado crearEmpleadoBase(Long sueldo, int horasBase, BigDecimal valorExtra, TipoPagoHoraExtra tipo) {
        return Empleado.builder()
                .id(7L)
                .nombre("Juan")
                .apellido("Pérez")
                .dni("35111222")
                .sueldoBaseCentavos(sueldo)
                .horasMensualesBase(horasBase)
                .valorHoraExtra(valorExtra)
                .tipoHoraExtra(tipo)
                .jefe(jefe)
                .build();
    }

    private Empleado crearEmpleadoConSueldoPlanificado() {
        Empleado emp = crearEmpleadoBase(90_000_000L, 144, BigDecimal.ONE, TipoPagoHoraExtra.MULTIPLICADOR_HORA);
        LocalDate fechaSueldo = LocalDate.of(2026, 12, 10);

        Gasto plantilla = Gasto.builder()
                .montoCentavos(emp.getSueldoBaseCentavos())
                .descripcion("Sueldo - Juan Pérez")
                .fecha(fechaSueldo.atStartOfDay())
                .tercero("Juan Pérez")
                .cuentaOrigen(cuentaOrigen)
                .cuentaDestino(cuentaExterna)
                .categoria(categoria)
                .metodoPago(metodoPago)
                .build();

        PagoInfinito pagoSueldo = PagoInfinito.builder()
                .fecha(fechaSueldo)
                .activo(true)
                .frecuencia(ChronoUnit.MONTHS)
                .empleado(emp)
                .build();
        pagoSueldo.agregarTransaccion(plantilla);

        emp.setPagoPlanificadoSueldo(pagoSueldo);
        return emp;
    }
}