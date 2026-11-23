package com.doulos.wallet.service;

import com.doulos.wallet.dto.cuenta.CrearCuentaDTO;
import com.doulos.wallet.dto.cuenta.CuentaResponseDTO;
import com.doulos.wallet.entity.Cuenta;
import com.doulos.wallet.entity.Usuario;
import com.doulos.wallet.entity.enums.TipoCuenta;
import com.doulos.wallet.repository.CuentaRepository;
import com.doulos.wallet.repository.UsuarioRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TransaccionService transaccionService;

    @InjectMocks
    private CuentaService cuentaService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Santiago")
                .build();
    }

    @Nested
    @DisplayName("Alta de Cuenta (Particiones Válidas y Fronteras de Saldo)")
    class CrearCuentaValidasTests {

        @Test
        void crearCuenta_FronteraSaldoCero_DeberiaGuardarEnCeroSinTransaccion() {
            CrearCuentaDTO dto = new CrearCuentaDTO("Efectivo", TipoCuenta.EFECTIVO, 0L);

            when(cuentaRepository.existsByNombreAndUsuarioId("Efectivo", 1L)).thenReturn(false);
            when(usuarioRepository.getReferenceById(1L)).thenReturn(usuario);
            simularGuardadoCuenta(10L);

            CuentaResponseDTO response = cuentaService.crearCuenta(dto, 1L);

            assertEquals(10L, response.id());
            assertEquals("Efectivo", response.nombre());
            assertEquals(0L, response.saldoCentavos());
            assertEquals(TipoCuenta.EFECTIVO, response.tipo());
            verify(transaccionService, never()).registrarIngresoInicial(any(), anyLong(), anyLong());
        }

        @Test
        void crearCuenta_FronteraSaldoMinimoPositivoUnCentavo_DeberiaRegistrarIngresoInicial() {
            CrearCuentaDTO dto = new CrearCuentaDTO("Billetera", TipoCuenta.CUENTA_GENERAL, 1L);

            when(cuentaRepository.existsByNombreAndUsuarioId("Billetera", 1L)).thenReturn(false);
            when(usuarioRepository.getReferenceById(1L)).thenReturn(usuario);
            simularGuardadoCuenta(15L);

            doAnswer(invocation -> {
                Cuenta cuenta = invocation.getArgument(0);
                cuenta.acreditar(invocation.getArgument(1));
                return null;
            }).when(transaccionService).registrarIngresoInicial(any(Cuenta.class), eq(1L), eq(1L));

            CuentaResponseDTO response = cuentaService.crearCuenta(dto, 1L);

            assertEquals(1L, response.saldoCentavos());
            verify(transaccionService).registrarIngresoInicial(any(Cuenta.class), eq(1L), eq(1L));
        }

        @Test
        void crearCuenta_ConIngresoInicial_DeberiaRegistrarIngresoYAcreditarSaldo() {
            CrearCuentaDTO dto = new CrearCuentaDTO("Banco Galicia", TipoCuenta.CUENTA_CORRIENTE, 150_000L);

            when(cuentaRepository.existsByNombreAndUsuarioId("Banco Galicia", 1L)).thenReturn(false);
            when(usuarioRepository.getReferenceById(1L)).thenReturn(usuario);
            simularGuardadoCuenta(20L);

            doAnswer(invocation -> {
                Cuenta cuenta = invocation.getArgument(0);
                cuenta.acreditar(invocation.getArgument(1));
                return null;
            }).when(transaccionService).registrarIngresoInicial(any(Cuenta.class), eq(150_000L), eq(1L));

            CuentaResponseDTO response = cuentaService.crearCuenta(dto, 1L);

            assertEquals(20L, response.id());
            assertEquals(150_000L, response.saldoCentavos());
            verify(transaccionService).registrarIngresoInicial(any(Cuenta.class), eq(150_000L), eq(1L));
        }
    }

    @Nested
    @DisplayName("Validaciones y Particiones Inválidas en Alta")
    class CrearCuentaInvalidasTests {

        @Test
        void crearCuenta_TipoONombreExterna_DeberiaLanzarExcepcion() {
            CrearCuentaDTO dtoTipoExterna = new CrearCuentaDTO("Oculta", TipoCuenta.EXTERNA, 0L);
            CrearCuentaDTO dtoNombreReservado = new CrearCuentaDTO("__CUENTA_EXTERNA__", TipoCuenta.EFECTIVO, 0L);

            assertThrows(IllegalArgumentException.class, () -> cuentaService.crearCuenta(dtoTipoExterna, 1L));
            assertThrows(IllegalArgumentException.class, () -> cuentaService.crearCuenta(dtoNombreReservado, 1L));
            verify(cuentaRepository, never()).save(any());
        }

        @Test
        void crearCuenta_NombreDuplicado_DeberiaLanzarExcepcion() {
            CrearCuentaDTO dto = new CrearCuentaDTO("Efectivo", TipoCuenta.EFECTIVO, 10_000L);

            when(cuentaRepository.existsByNombreAndUsuarioId("Efectivo", 1L)).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> cuentaService.crearCuenta(dto, 1L));
            verify(cuentaRepository, never()).save(any());
        }

        @Test
        void crearCuenta_FronteraSaldoNegativo_DeberiaLanzarExcepcion() {
            CrearCuentaDTO dtoFronteraNegativa = new CrearCuentaDTO("Efectivo", TipoCuenta.EFECTIVO, -1L);
            CrearCuentaDTO dtoNegativoMayor = new CrearCuentaDTO("Efectivo", TipoCuenta.EFECTIVO, -500L);

            when(cuentaRepository.existsByNombreAndUsuarioId("Efectivo", 1L)).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> cuentaService.crearCuenta(dtoFronteraNegativa, 1L));
            assertThrows(IllegalArgumentException.class, () -> cuentaService.crearCuenta(dtoNegativoMayor, 1L));
            verify(cuentaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Listado de Cuentas Activas")
    class ListarCuentasTests {

        @Test
        void listarCuentasActivas_DeberiaExcluirTipoExterna() {
            Cuenta efectivo = Cuenta.builder()
                    .id(1L).nombre("Efectivo").tipo(TipoCuenta.EFECTIVO).saldoCentavos(50_000L).usuario(usuario).build();
            Cuenta banco = Cuenta.builder()
                    .id(2L).nombre("Banco").tipo(TipoCuenta.CUENTA_CORRIENTE).saldoCentavos(120_000L).usuario(usuario).build();

            when(cuentaRepository.findByUsuarioIdAndTipoNotAndEstadoTrue(1L, TipoCuenta.EXTERNA))
                    .thenReturn(List.of(efectivo, banco));

            List<CuentaResponseDTO> resultado = cuentaService.listarCuentasActivas(1L);

            assertEquals(2, resultado.size());
            assertTrue(resultado.stream().noneMatch(c -> c.tipo() == TipoCuenta.EXTERNA));
        }

        @Test
        void listarCuentasActivas_FronteraSinCuentas_RetornaListaVacia() {
            when(cuentaRepository.findByUsuarioIdAndTipoNotAndEstadoTrue(1L, TipoCuenta.EXTERNA))
                    .thenReturn(Collections.emptyList());

            List<CuentaResponseDTO> resultado = cuentaService.listarCuentasActivas(1L);

            assertTrue(resultado.isEmpty());
        }
    }

    @Nested
    @DisplayName("Consulta por ID y Baja Lógica")
    class ConsultarYDesactivarTests {

        @Test
        void obtenerPorId_CuentaPropia_RetornaDto() {
            Cuenta cuenta = Cuenta.builder()
                    .id(10L).nombre("Mercado Pago").tipo(TipoCuenta.CUENTA_GENERAL)
                    .saldoCentavos(50_000L).estado(true).usuario(usuario).build();
            when(cuentaRepository.findById(10L)).thenReturn(Optional.of(cuenta));

            CuentaResponseDTO resultado = cuentaService.obtenerPorId(10L, 1L);

            assertEquals(10L, resultado.id());
            assertEquals("Mercado Pago", resultado.nombre());
        }

        @Test
        void desactivar_CuentaPropia_CambiaEstadoAFalseYGuarda() {
            Cuenta cuenta = Cuenta.builder()
                    .id(10L).nombre("Efectivo").tipo(TipoCuenta.EFECTIVO)
                    .saldoCentavos(0L).estado(true).usuario(usuario).build();
            when(cuentaRepository.findById(10L)).thenReturn(Optional.of(cuenta));

            cuentaService.desactivar(10L, 1L);

            assertFalse(cuenta.isEstado());
            verify(cuentaRepository).save(cuenta);
        }

        @Test
        void obtenerODesactivar_InexistenteOAjena_LanzaExcepcion() {
            when(cuentaRepository.findById(404L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> cuentaService.obtenerPorId(404L, 1L));

            Usuario otro = Usuario.builder().id(99L).build();
            Cuenta ajena = Cuenta.builder()
                    .id(20L).tipo(TipoCuenta.EFECTIVO).usuario(otro).build();
            when(cuentaRepository.findById(20L)).thenReturn(Optional.of(ajena));
            assertThrows(AccessDeniedException.class, () -> cuentaService.desactivar(20L, 1L));
        }
    }

    private void simularGuardadoCuenta(Long idGenerado) {
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> {
            Cuenta c = invocation.getArgument(0);
            c.setId(idGenerado);
            return c;
        });
    }
}