package com.doulos.wallet.scheduler;

import com.doulos.wallet.service.PagoPlanificadoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PagoPlanificadoSchedulerTest {

    @Mock
    private PagoPlanificadoService pagoPlanificadoService;

    @InjectMocks
    private PagoPlanificadoScheduler scheduler;

    @Test
    void procesarPagosDelDia_InvocaServicioConFechaActualArgentina() {
        LocalDate hoyEsperado = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));

        scheduler.procesarPagosDelDia();

        verify(pagoPlanificadoService).ejecutarPendientes(hoyEsperado);
    }

    @Test
    void procesarPagosDelDia_SiFallaElServicio_NoPropagaExcepcion() {
        LocalDate hoyEsperado = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));
        doThrow(new RuntimeException("Fallo de conexión"))
                .when(pagoPlanificadoService).ejecutarPendientes(hoyEsperado);

        assertDoesNotThrow(() -> scheduler.procesarPagosDelDia());
    }
}