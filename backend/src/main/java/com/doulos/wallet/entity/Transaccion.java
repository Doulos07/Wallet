package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.EstadoConciliacion;
import com.doulos.wallet.entity.enums.EstadoTransaccion;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "transacciones")
// Guarda todas las clases hijas en esta misma tabla
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
// Columna que diferenciará si es ingreso, gasto o transferencia
@DiscriminatorColumn(name = "tipo_movimiento", discriminatorType = DiscriminatorType.STRING)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public abstract class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monto_centavos", nullable = false)
    private Long montoCentavos;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 150)
    private String tercero;

    private Integer garantiaMeses;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_bancario", nullable = false)
    private EstadoConciliacion estadoBancario = EstadoConciliacion.LIQUIDADO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_negocio", nullable = false)
    private EstadoTransaccion estadoNegocio = EstadoTransaccion.VIGENTE;

    // Relaciones base
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_origen_id", nullable = false)
    private Cuenta cuentaOrigen;

    // Guarda la cuenta destinó (en transferencias) o la cuenta externa (en gastos/ingresos)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_destino_id", nullable = false)
    private Cuenta cuentaDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    @OneToOne(mappedBy = "transaccion", cascade = CascadeType.ALL)
    private Asiento asiento;

    // Cada transacción registrada sabe qué plantilla automática la originó (puede ser null si fue manual)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_planificado_id")
    private PagoPlanificado pagoPlanificado;

    //======================

    // En Transaccion.java (reemplaza la declaración abstract por esta)
    public Transaccion clonarParaEjecucion(LocalDateTime fechaEjecucion) {
        throw new UnsupportedOperationException("Este tipo de movimiento no soporta pagos planificados.");
    }

    // Cada hijo devuelve su propio identificador de tipo
    public abstract String getTipoMovimiento();

    // Arma la cabecera del asiento para reutilizar en los hijos
    protected Asiento crearAsientoBase() {
        return Asiento.builder()
                .fecha(this.fecha)
                .transaccion(this)
                .build();
    }

    // En Transaccion.java
    public void anular() {
        if (this.estadoNegocio == EstadoTransaccion.ANULADO) {
            throw new IllegalStateException("La transacción ya se encuentra anulada.");
        }
        this.estadoNegocio = EstadoTransaccion.ANULADO;
    }

    // Obliga a cada subclase a definir sus líneas
    public abstract Asiento generarAsiento();


}