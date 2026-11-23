package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.TipoMovimiento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lineas_asiento")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LineaAsiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "monto_centavos")
    private Long montoCentavos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "asiento_id")
    private Asiento asiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "cuenta_id")
    private Cuenta cuenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "tipo_movimiento", length = 50)
    private TipoMovimiento tipoMovimiento;

    // ===================

    // Aplica el efecto de esta línea sobre el saldo de su cuenta
    public void impactarEnCuenta() {
        if (this.tipoMovimiento == TipoMovimiento.DEBITO) {
            this.cuenta.debitar(this.montoCentavos);
        } else {
            this.cuenta.acreditar(this.montoCentavos);
        }
    }
}
