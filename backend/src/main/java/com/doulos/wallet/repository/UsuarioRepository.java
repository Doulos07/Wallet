package com.doulos.wallet.repository;

import com.doulos.wallet.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // 1. Para Spring Security: Trae la entidad completa si la encuentra
    Optional<Usuario> findByEmail(String email);

    // 2. Para tu validación (Service): Solo devuelve true/false (es mucho más rápido en SQL)
    boolean existsByEmail(String email);

}
