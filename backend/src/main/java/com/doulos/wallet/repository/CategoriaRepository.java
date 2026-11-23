package com.doulos.wallet.repository;

import com.doulos.wallet.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByEstadoTrue();
    boolean existsByNombreIgnoreCase(String nombre);
}
