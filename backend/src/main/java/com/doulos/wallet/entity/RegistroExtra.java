package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.EstadoConciliacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "registros_extra")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RegistroExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "cantidad_horas_extra", precision = 6, scale = 2)
    private BigDecimal cantidadHorasExtra;

    private String descripcion;

    @Column(nullable = false, name = "monto_calculado_centavos")
    private Long montoCalculadoCentavos;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "estado_liquidacion")
    private EstadoConciliacion estadoLiquidacion = EstadoConciliacion.NO_LIQUIDADO;

    // Gasto real generado al liquidarse
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Gasto pago;

    // Pago único programado que agrupa este extra
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_planificado_id")
    private PagoPlanificado pagoPlanificado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "empleado_id")
    private Empleado empleado;

    public void liquidarCon(Gasto gastoExtras) {
        this.pago = gastoExtras;
        this.estadoLiquidacion = EstadoConciliacion.LIQUIDADO;
    }
}