package com.doulos.wallet.scheduler;

import com.doulos.wallet.service.PagoPlanificadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagoPlanificadoScheduler {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final PagoPlanificadoService pagoPlanificadoService;

    // Ejecuta todos los días a las 00:05:00 hora Argentina
    @Scheduled(cron = "0 5 0 * * *", zone = "America/Argentina/Buenos_Aires")
    public void procesarPagosDelDia() {
        LocalDate hoy = LocalDate.now(ZONA_ARGENTINA);
        log.info("Iniciando ejecución automática de pagos planificados para: {}", hoy);

        try {
            pagoPlanificadoService.ejecutarPendientes(hoy);
            log.info("Ejecución automática finalizada correctamente para: {}", hoy);
        } catch (Exception e) {
            log.error("Error al procesar pagos planificados ({}): {}", hoy, e.getMessage(), e);
        }
    }
}