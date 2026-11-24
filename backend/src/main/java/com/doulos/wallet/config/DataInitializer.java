package com.doulos.wallet.config;

import com.doulos.wallet.entity.Categoria;
import com.doulos.wallet.entity.MetodoPago;
import com.doulos.wallet.repository.CategoriaRepository;
import com.doulos.wallet.repository.MetodoPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoriaRepository categoriaRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        asegurarIndiceCuentasActivas();
        cargarCategoriasIniciales();
        cargarMetodosPagoIniciales();
    }

    private void asegurarIndiceCuentasActivas() {
        jdbcTemplate.execute("""
                ALTER TABLE cuentas
                DROP CONSTRAINT IF EXISTS ukqejv97kgk2upcbetwfipqyedj
                """);

        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_cuentas_activas_usuario_nombre
                ON cuentas (usuario_id, LOWER(nombre))
                WHERE estado = true
                """);
    }

    private void cargarCategoriasIniciales() {
        if (categoriaRepository.count() > 0) {
            return;
        }

        List<String> nombres = List.of(
                "Alimentación",
                "Transporte",
                "Servicios",
                "Vivienda",
                "Salud",
                "Entretenimiento",
                "Educación",
                "Sueldos y Nómina",
                "Ingresos / Freelance",
                "Otros"
        );

        List<Categoria> categorias = nombres.stream()
                .map(nombre -> Categoria.builder().nombre(nombre).estado(true).build())
                .toList();

        categoriaRepository.saveAll(categorias);
        log.info("Catálogo inicial cargado: {} categorías creadas.", categorias.size());
    }

    private void cargarMetodosPagoIniciales() {
        if (metodoPagoRepository.count() > 0) {
            return;
        }

        List<String> nombres = List.of(
                "Efectivo",
                "Transferencia Bancaria",
                "Tarjeta de Débito",
                "Tarjeta de Crédito",
                "Billetera Virtual / QR"
        );

        List<MetodoPago> metodos = nombres.stream()
                .map(nombre -> MetodoPago.builder().nombre(nombre).estado(true).build())
                .toList();

        metodoPagoRepository.saveAll(metodos);
        log.info("Catálogo inicial cargado: {} métodos de pago creados.", metodos.size());
    }
}