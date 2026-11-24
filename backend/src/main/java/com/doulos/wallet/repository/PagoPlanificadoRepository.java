package com.doulos.wallet.repository;

import com.doulos.wallet.entity.PagoPlanificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PagoPlanificadoRepository extends JpaRepository<PagoPlanificado, Long> {

    @Query("""
            SELECT DISTINCT p FROM PagoPlanificado p
            JOIN FETCH p.transaccionesGeneradas t
            JOIN FETCH t.cuentaOrigen co
            LEFT JOIN FETCH t.cuentaDestino cd
            WHERE co.usuario.id = :usuarioId AND p.activo = true
            ORDER BY p.fecha ASC
            """)
    List<PagoPlanificado> findActivosConDetalleByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT DISTINCT p FROM PagoPlanificado p
            JOIN FETCH p.transaccionesGeneradas t
            JOIN FETCH t.cuentaOrigen co
            LEFT JOIN FETCH t.cuentaDestino cd
            WHERE p.activo = true
            """)
    List<PagoPlanificado> findAllActivosConDetalle();

    @Query("""
            SELECT DISTINCT p FROM PagoPlanificado p
            LEFT JOIN FETCH p.transaccionesGeneradas t
            WHERE p.activo = true AND p.fecha <= :hoy
            """)
    List<PagoPlanificado> findEjecutablesHasta(@Param("hoy") LocalDate hoy);
}