package com.doulos.wallet.entity;

import com.doulos.wallet.entity.enums.EstadoConciliacion;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pagos_planificados")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_pago_planificado", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("PAGO_PLANIFICADO")
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PagoPlanificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    private boolean activo;

    // Una plantilla genera muchas transacciones históricas a lo largo del tiempo
    @Builder.Default
    @OneToMany(mappedBy = "pagoPlanificado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaccion> transaccionesGeneradas = new ArrayList<>();

    @OneToOne(mappedBy = "pagoPlanificadoSueldo")
    private Empleado empleado;

    // Extras agrupados cuando este pago único liquida horas extras del período
    @Builder.Default
    @OneToMany(mappedBy = "pagoPlanificado")
    private List<RegistroExtra> registrosExtra = new ArrayList<>();

    // ====================

    // Vincula una transacción (plantilla o ejecutada) al pago planificado
    public void agregarTransaccion(Transaccion transaccion) {
        transaccion.setPagoPlanificado(this);
        this.transaccionesGeneradas.add(transaccion);
    }

    // Vincula un registro extra al pago único que lo liquidará
    public void agregarRegistroExtra(RegistroExtra extra) {
        extra.setPagoPlanificado(this);
        this.registrosExtra.add(extra);
    }

    // Incrementa el monto de la plantilla al acumular nuevos extras en el período
    public void sumarAPlantilla(long montoCentavos) {
        Transaccion plantilla = obtenerPlantilla();
        if (plantilla != null) {
            plantilla.setMontoCentavos(plantilla.getMontoCentavos() + montoCentavos);
        }
    }

    // Obtiene la transacción moldé (la única sin asiento y que no está pendiente de autorizar)
    public Transaccion obtenerPlantilla() {
        return this.transaccionesGeneradas.stream()
                .filter(t -> t.getAsiento() == null
                        && t.getEstadoBancario() != EstadoConciliacion.NO_LIQUIDADO)
                .findFirst()
                .orElse(null);
    }

    // Verifica que la transacción plantilla del pago pertenezca al usuario
    public void validarPertenencia(Long usuarioId) {
        Transaccion plantilla = obtenerPlantilla();
        if (plantilla == null || !plantilla.getCuentaOrigen().getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("No tenés permiso para modificar este pago planificado.");
        }
    }

    // Clona la plantilla, ajusta montos de nómina y deja el movimiento pendiente de autorización
    public void ejecutar(LocalDate hoy) {
        Transaccion plantilla = obtenerPlantilla();
        if (plantilla == null) {
            return;
        }
        long montoFinal = plantilla.getMontoCentavos();

        if (this.empleado != null) {
            montoFinal = Math.max(0L, montoFinal - this.empleado.descontarAdelantosPendientes());
        }

        if (montoFinal > 0L) {
            Transaccion nueva = plantilla.clonarParaEjecucion(hoy.atStartOfDay());
            nueva.setMontoCentavos(montoFinal);
            nueva.setEstadoBancario(EstadoConciliacion.NO_LIQUIDADO);
            nueva.setAsiento(null);

            if (!this.registrosExtra.isEmpty()) {
                Gasto gastoExtras = (Gasto) nueva;
                this.registrosExtra.forEach(extra -> extra.setPago(gastoExtras));
            }

            agregarTransaccion(nueva);
        }

        actualizarTrasEjecucion();
    }

    public boolean debeEjecutarse(LocalDate hoy) {
        return fecha.isEqual(hoy);
    }

    // Clase concreta = pago único: se desactiva tras ejecutarse
    public void actualizarTrasEjecucion() {
        this.activo = false;
    }

    // Getters polimórficos por defecto para PagoPlanificadoResponseDTO
    public String getTipoRecurrencia() { return "PAGO_PLANIFICADO"; }
    public ChronoUnit getFrecuencia() { return null; }
    public LocalDate getFechaFin() { return null; }
    public Integer getCantidadEventos() { return null; }
    public Integer getEventoActual() { return null; }
}