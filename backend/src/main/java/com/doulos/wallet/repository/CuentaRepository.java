package com.doulos.wallet.repository;

import com.doulos.wallet.entity.Cuenta;
import com.doulos.wallet.entity.enums.TipoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    // Spring genera un "SELECT * FROM cuentas WHERE usuario_id = ?"
    List<Cuenta> findByUsuarioId(Long usuarioId);

    // Spring traduce esto a: SELECT EXISTS(SELECT 1 FROM cuentas WHERE nombre = ? AND usuario_id = ?)
    boolean existsByNombreAndUsuarioId(String nombre, Long usuarioId);

    List<Cuenta> findByUsuarioIdAndEstadoTrue(Long usuarioId);

    Optional<Cuenta> findByUsuarioIdAndTipo(Long usuarioId, TipoCuenta tipoCuenta);

    // Trae solo las cuentas reales y activas del usuario
    List<Cuenta> findByUsuarioIdAndTipoNotAndEstadoTrue(Long usuarioId, TipoCuenta tipoExcluido);

    boolean existsByNombreIgnoreCaseAndUsuarioIdAndEstadoTrue(String nombre, Long usuarioId);
}
