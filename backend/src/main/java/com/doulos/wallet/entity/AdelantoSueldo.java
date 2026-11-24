package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.TipoPagoHoraExtra;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "adelantos_sueldo")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdelantoSueldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, name = "monto_centavos")
    private Long montoCentavos;

    private String motivo;

    @Builder.Default
    private boolean descontado = false;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(nullable = false, name = "pago_id")
    private Gasto pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "empleado_id")
    private Empleado empleado;

    // ============================

    public void marcarComoDescontado() {
        this.descontado = true;
    }
}
