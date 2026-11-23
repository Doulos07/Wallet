package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.TipoPagoHoraExtra;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "empleados")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false)
    private String dni;

    @Column(nullable = false, name = "sueldo_base_centavos")
    private Long sueldoBaseCentavos;

    @Column(nullable = false, name = "horas_mensuales_base")
    private Integer horasMensualesBase;

    @Builder.Default
    @Column(nullable = false, name = "valor_hora_extra", precision = 12, scale = 2)
    private BigDecimal valorHoraExtra = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "tipo_hora_extra")
    private TipoPagoHoraExtra tipoHoraExtra;

    @Builder.Default
    @Column(nullable = false, name = "dia_pago_sueldo")
    private Integer diaPagoSueldo = 10;

    @Builder.Default
    @Column(nullable = false, name = "dia_pago_extras")
    private Integer diaPagoExtras = 20;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    // ==============================
    // RELACIONES
    // ==============================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "usuario_id")
    private Usuario jefe;

    // Pago recurrente (PagoInfinito) del sueldo base para el día 10 de cada mes
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_planificado_id")
    private PagoPlanificado pagoPlanificadoSueldo;

    @Builder.Default
    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdelantoSueldo> adelantoSueldos = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "empleado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistroExtra> registroExtras = new ArrayList<>();

    // ==============================
    // MÉTODOS DE DOMINIO
    // ==============================

    public long calcularMontoExtraCentavos(BigDecimal cantidadHoras, Long montoManualCentavos) {
        return this.tipoHoraExtra.calcularMontoCentavos(this, cantidadHoras, montoManualCentavos);
    }

    public void agregarRegistroExtra(RegistroExtra registro) {
        registro.setEmpleado(this);
        this.registroExtras.add(registro);
    }

    public void agregarAdelantoSueldo(AdelantoSueldo adelanto) {
        adelanto.setEmpleado(this);
        this.adelantoSueldos.add(adelanto);
    }

    // Verifica que el empleado pertenezca al usuario autenticado
    public void validarPertenencia(Long jefeId) {
        if (!this.jefe.getId().equals(jefeId)) {
            throw new AccessDeniedException("No tenés permiso para operar sobre este empleado.");
        }
    }

    // En Empleado.java (agregar estos métodos de dominio)
    public long descontarAdelantosPendientes() {
        List<AdelantoSueldo> pendientes = this.adelantoSueldos.stream()
                .filter(a -> !a.isDescontado())
                .toList();

        long total = pendientes.stream()
                .mapToLong(AdelantoSueldo::getMontoCentavos)
                .sum();
        pendientes.forEach(AdelantoSueldo::marcarComoDescontado);
        return total;
    }

    // Busca el pago único de extras pendiente de ejecución para el período actual
    public Optional<PagoPlanificado> obtenerPagoExtrasActivo() {
        return this.registroExtras.stream()
                .map(RegistroExtra::getPagoPlanificado)
                .filter(p -> p != null && p.isActivo())
                .findFirst();
    }

    // Da de baja al empleado y desactiva sus pagos pendientes
    public void darDeBaja() {
        this.activo = false;
        if (this.pagoPlanificadoSueldo != null) {
            this.pagoPlanificadoSueldo.setActivo(false);
        }
        obtenerPagoExtrasActivo().ifPresent(pago -> pago.setActivo(false));
    }

    public void asignarPagoSueldo(PagoPlanificado pagoSueldo) {
        pagoSueldo.setEmpleado(this);
        this.pagoPlanificadoSueldo = pagoSueldo;
    }
}