package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.TipoCuenta;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cuentas", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "nombre"}))
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Builder.Default
    @Column(nullable = false)
    private Long saldoCentavos = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoCuenta tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Builder.Default
    private boolean estado = true;

    @Builder.Default
    @OneToMany(mappedBy = "cuentaOrigen", fetch = FetchType.LAZY)
    private List<Transaccion> transacciones = new ArrayList<>();

    // ===================

    // Crea la cuenta oculta de contrapartida para partida doble
    public static Cuenta crearCuentaExterna(Usuario usuario) {
        return Cuenta.builder()
                .nombre("__CUENTA_EXTERNA__")
                .tipo(TipoCuenta.EXTERNA)
                .usuario(usuario)
                .build();
    }

    // Indica si es la cuenta interna usada para cuadrar ingresos y gastos
    public boolean esExterna() {
        return this.tipo == TipoCuenta.EXTERNA;
    }

    // Suma dinero al saldo actual de la cuenta
    public void acreditar(Long montoCentavos) {
        validarMontoPositivo(montoCentavos);
        this.saldoCentavos += montoCentavos;
    }

    // Resta dinero del saldo actual de la cuenta
    public void debitar(Long montoCentavos) {
        validarMontoPositivo(montoCentavos);
        this.saldoCentavos -= montoCentavos;
    }

    private void validarMontoPositivo(Long montoCentavos) {
        if (montoCentavos == null || montoCentavos <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
    }

    // En Cuenta.java
    public void validarOperablePor(Long usuarioId) {
        if (!this.usuario.getId().equals(usuarioId)) {
            throw new AccessDeniedException("No tenés permiso para operar sobre esta cuenta.");
        }
        if (this.esExterna()) {
            throw new IllegalArgumentException("No se puede operar directamente sobre la cuenta externa.");
        }
    }
}