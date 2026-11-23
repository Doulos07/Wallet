package com.doulos.wallet.repository;

import com.doulos.wallet.entity.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {
    List<MetodoPago> findByEstadoTrue();
    boolean existsByNombreIgnoreCase(String nombre);
}
