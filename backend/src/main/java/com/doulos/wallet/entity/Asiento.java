package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.TipoMovimiento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asientos")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // Solo el asiento original apunta a la transacción; el contra-asiento queda en null
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id", unique = true)
    private Transaccion transaccion;

    @Builder.Default
    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaAsiento> lineas = new ArrayList<>();

    // Referencia al contra-asiento cuando este movimiento fue anulado
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contra_asiento_id")
    private Asiento contraAsiento;

    // Crea y vincula una línea al asiento actual
    public void agregarLinea(Cuenta cuenta, Long montoCentavos, TipoMovimiento tipoMovimiento) {
        LineaAsiento linea = LineaAsiento.builder()
                .asiento(this)
                .cuenta(cuenta)
                .montoCentavos(montoCentavos)
                .tipoMovimiento(tipoMovimiento)
                .build();
        this.lineas.add(linea);
    }

    // Impacta el saldo de todas las cuentas involucradas en el asiento
    public void impactarSaldos() {
        this.lineas.forEach(LineaAsiento::impactarEnCuenta);
    }

    // Genera el contra-asiento invirtiendo débitos y créditos y lo vincula al asiento original
    public Asiento generarContraAsiento() {
        Asiento contra = Asiento.builder()
                .fecha(LocalDateTime.now())
                .build();

        for (LineaAsiento linea : this.lineas) {
            TipoMovimiento tipoInvertido = (linea.getTipoMovimiento() == TipoMovimiento.DEBITO)
                    ? TipoMovimiento.CREDITO
                    : TipoMovimiento.DEBITO;

            LineaAsiento contraLinea = LineaAsiento.builder()
                    .cuenta(linea.getCuenta())
                    .montoCentavos(linea.getMontoCentavos())
                    .tipoMovimiento(tipoInvertido)
                    .asiento(contra)
                    .build();

            contra.getLineas().add(contraLinea);
        }

        this.contraAsiento = contra;
        return contra;
    }
}