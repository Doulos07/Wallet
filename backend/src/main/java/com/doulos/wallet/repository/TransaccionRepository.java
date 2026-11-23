package com.doulos.wallet.repository;

import com.doulos.wallet.entity.Transaccion;
import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    // Busca el historial ordenado de más reciente a más antiguo
    List<Transaccion> findByCuentaOrigenIdOrderByFechaDesc(Long cuentaId);

    // Consulta JPQL personalizada para un rango de fechas
    @Query("SELECT t FROM Transaccion t WHERE t.cuentaOrigen.id = :cuentaId AND t.fecha BETWEEN :inicio AND :fin")
    List<Transaccion> buscarHistorialPorFecha(@Param("cuentaId") Long cuentaId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // Lista transacciones del usuario filtrando por cuenta, estados y rango de fechas
    @EntityGraph(attributePaths = {"cuentaOrigen", "cuentaDestino", "categoria", "metodoPago"})
    @Query("""
            SELECT t FROM Transaccion t
            WHERE t.cuentaOrigen.usuario.id = :usuarioId
              AND (:cuentaId IS NULL OR t.cuentaOrigen.id = :cuentaId OR t.cuentaDestino.id = :cuentaId)
              AND (:estadoNegocio IS NULL OR t.estadoNegocio = :estadoNegocio)
              AND (:estadoBancario IS NULL OR t.estadoBancario = :estadoBancario)
              AND (:desde IS NULL OR t.fecha >= :desde)
              AND (:hasta IS NULL OR t.fecha <= :hasta)
            """)
    Page<Transaccion> buscarPorUsuarioConFiltros(
            @Param("usuarioId") Long usuarioId,
            @Param("cuentaId") Long cuentaId,
            @Param("estadoNegocio") EstadoTransaccion estadoNegocio,
            @Param("estadoBancario") EstadoConciliacion estadoBancario,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
