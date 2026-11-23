package com.doulos.wallet.service;

import com.doulos.wallet.dto.empleado.*;
import com.doulos.wallet.entity.*;
import com.doulos.wallet.repository.CategoriaRepository;
import com.doulos.wallet.repository.EmpleadoRepository;
import com.doulos.wallet.repository.MetodoPagoRepository;
import com.doulos.wallet.repository.PagoPlanificadoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final PagoPlanificadoRepository pagoPlanificadoRepository;
    private final CuentaService cuentaService;
    private final CategoriaRepository categoriaRepository;
    private final MetodoPagoRepository metodoPagoRepository;

    @Transactional
    public EmpleadoResponseDTO crear(CrearEmpleadoDTO dto, Usuario jefe) {
        Cuenta origen = cuentaService.obtenerCuentaValida(dto.cuentaOrigenId(), jefe.getId());
        Cuenta externa = cuentaService.obtenerCuentaExterna(jefe.getId());
        Categoria categoria = obtenerCategoria(dto.categoriaId());
        MetodoPago metodoPago = obtenerMetodoPago(dto.metodoPagoId());

        LocalDate fechaPrimerSueldo = calcularProximaFecha(dto.diaPagoSueldo(), 10);
        String nombreCompleto = dto.nombre() + " " + dto.apellido();

        // Arma la plantilla de gasto del sueldo sin asiento contable
        Gasto plantillaSueldo = Gasto.builder()
                .montoCentavos(dto.sueldoBaseCentavos())
                .descripcion("Sueldo - " + nombreCompleto)
                .fecha(fechaPrimerSueldo.atStartOfDay())
                .tercero(nombreCompleto)
                .cuentaOrigen(origen)
                .cuentaDestino(externa)
                .categoria(categoria)
                .metodoPago(metodoPago)
                .build();

        PagoInfinito pagoSueldo = PagoInfinito.builder()
                .fecha(fechaPrimerSueldo)
                .activo(true)
                .frecuencia(ChronoUnit.MONTHS)
                .build();
        pagoSueldo.agregarTransaccion(plantillaSueldo);

        Empleado empleado = dto.toEntity(jefe);
        empleado.setPagoPlanificadoSueldo(pagoSueldo);

        return EmpleadoResponseDTO.fromEntity(empleadoRepository.save(empleado));
    }

    @Transactional(readOnly = true)
    public List<EmpleadoResponseDTO> listarActivos(Long jefeId) {
        return empleadoRepository.findByJefeIdAndActivoTrue(jefeId).stream()
                .map(EmpleadoResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void registrarExtra(Long empleadoId, CrearRegistroExtraDTO dto, Long jefeId) {
        Empleado empleado = obtenerEmpleadoActivo(empleadoId, jefeId);

        long montoCentavos = empleado.calcularMontoExtraCentavos(
                dto.cantidadHorasExtra(),
                dto.montoManualCentavos()
        );

        RegistroExtra extra = RegistroExtra.builder()
                .fecha(dto.fecha())
                .cantidadHorasExtra(dto.cantidadHorasExtra())
                .descripcion(dto.descripcion())
                .montoCalculadoCentavos(montoCentavos)
                .build();
        empleado.agregarRegistroExtra(extra);

        // Acumula en el pago único activo de extras o crea uno nuevo para el día configurado
        PagoPlanificado pagoExtras = empleado.obtenerPagoExtrasActivo()
                .map(existente -> {
                    existente.sumarAPlantilla(montoCentavos);
                    return existente;
                })
                .orElseGet(() -> crearPagoUnicoExtras(empleado, montoCentavos));

        pagoExtras.agregarRegistroExtra(extra);
        pagoPlanificadoRepository.save(pagoExtras);
    }

    @Transactional
    public void registrarAdelanto(Long empleadoId, CrearAdelantoSueldoDTO dto, Long jefeId) {
        Empleado empleado = obtenerEmpleadoActivo(empleadoId, jefeId);
        Cuenta origen = cuentaService.obtenerCuentaValida(dto.cuentaOrigenId(), jefeId);
        Cuenta externa = cuentaService.obtenerCuentaExterna(jefeId);
        Categoria categoria = obtenerCategoria(dto.categoriaId());
        MetodoPago metodoPago = obtenerMetodoPago(dto.metodoPagoId());

        // Registra el gasto real y su asiento contable al entregar el adelanto
        Gasto gastoAdelanto = Gasto.builder()
                .montoCentavos(dto.montoCentavos())
                .descripcion("Adelanto de sueldo - " + empleado.getNombre() + " " + empleado.getApellido())
                .fecha(dto.fecha().atStartOfDay())
                .tercero(empleado.getNombre() + " " + empleado.getApellido())
                .cuentaOrigen(origen)
                .cuentaDestino(externa)
                .categoria(categoria)
                .metodoPago(metodoPago)
                .build();
        gastoAdelanto.setAsiento(gastoAdelanto.generarAsiento());

        AdelantoSueldo adelanto = AdelantoSueldo.builder()
                .fecha(dto.fecha())
                .montoCentavos(dto.montoCentavos())
                .motivo(dto.motivo())
                .pago(gastoAdelanto)
                .build();

        empleado.agregarAdelantoSueldo(adelanto);
    }

    @Transactional
    public void desactivar(Long empleadoId, Long jefeId) {
        Empleado empleado = obtenerEmpleadoActivo(empleadoId, jefeId);
        empleado.darDeBaja();
    }

    private PagoPlanificado crearPagoUnicoExtras(Empleado empleado, long montoInicialCentavos) {
        Transaccion plantillaSueldo = empleado.getPagoPlanificadoSueldo().obtenerPlantilla();
        LocalDate fechaCobro = calcularProximaFecha(empleado.getDiaPagoExtras(), 20);
        String nombreCompleto = empleado.getNombre() + " " + empleado.getApellido();

        Gasto plantillaExtras = Gasto.builder()
                .montoCentavos(montoInicialCentavos)
                .descripcion("Horas extras - " + nombreCompleto)
                .fecha(fechaCobro.atStartOfDay())
                .tercero(nombreCompleto)
                .cuentaOrigen(plantillaSueldo.getCuentaOrigen())
                .cuentaDestino(plantillaSueldo.getCuentaDestino())
                .categoria(plantillaSueldo.getCategoria())
                .metodoPago(plantillaSueldo.getMetodoPago())
                .build();

        PagoPlanificado pagoUnico = PagoPlanificado.builder()
                .fecha(fechaCobro)
                .activo(true)
                .build();
        pagoUnico.agregarTransaccion(plantillaExtras);
        return pagoUnico;
    }

    private LocalDate calcularProximaFecha(Integer diaConfigurado, int diaPorDefecto) {
        int dia = Objects.requireNonNullElse(diaConfigurado, diaPorDefecto);
        LocalDate hoy = LocalDate.now();
        LocalDate candidata = hoy.withDayOfMonth(Math.min(dia, hoy.lengthOfMonth()));

        if (candidata.isBefore(hoy)) {
            LocalDate siguienteMes = hoy.plusMonths(1);
            candidata = siguienteMes.withDayOfMonth(Math.min(dia, siguienteMes.lengthOfMonth()));
        }
        return candidata;
    }

    @Transactional(readOnly = true)
    public EmpleadoResponseDTO obtenerPorId(Long empleadoId, Long jefeId) {
        Empleado empleado = obtenerEmpleadoPropio(empleadoId, jefeId);
        return EmpleadoResponseDTO.fromEntity(empleado);
    }

    @Transactional
    public EmpleadoResponseDTO actualizarCondiciones(Long empleadoId, ActualizarEmpleadoDTO dto, Long jefeId) {
        Empleado empleado = obtenerEmpleadoActivo(empleadoId, jefeId);

        empleado.setSueldoBaseCentavos(dto.sueldoBaseCentavos());
        empleado.setHorasMensualesBase(dto.horasMensualesBase());
        empleado.setValorHoraExtra(dto.valorHoraExtra());
        empleado.setTipoHoraExtra(dto.tipoHoraExtra());

        if (dto.diaPagoSueldo() != null) {
            empleado.setDiaPagoSueldo(dto.diaPagoSueldo());
        }
        if (dto.diaPagoExtras() != null) {
            empleado.setDiaPagoExtras(dto.diaPagoExtras());
        }

        // Sincroniza el nuevo sueldo base en la plantilla planificada
        if (empleado.getPagoPlanificadoSueldo() != null
                && empleado.getPagoPlanificadoSueldo().obtenerPlantilla() != null) {
            empleado.getPagoPlanificadoSueldo().obtenerPlantilla().setMontoCentavos(dto.sueldoBaseCentavos());
        }

        return EmpleadoResponseDTO.fromEntity(empleadoRepository.save(empleado));
    }

    private Empleado obtenerEmpleadoPropio(Long empleadoId, Long jefeId) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new EntityNotFoundException("Empleado no encontrado."));
        empleado.validarPertenencia(jefeId);
        return empleado;
    }

    private Empleado obtenerEmpleadoActivo(Long empleadoId, Long jefeId) {
        Empleado empleado = obtenerEmpleadoPropio(empleadoId, jefeId);
        if (!empleado.isActivo()) {
            throw new IllegalStateException("El empleado se encuentra dado de baja.");
        }
        return empleado;
    }

    private Categoria obtenerCategoria(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new EntityNotFoundException("Categoría no encontrada."));
    }

    private MetodoPago obtenerMetodoPago(Long metodoPagoId) {
        return metodoPagoRepository.findById(metodoPagoId)
                .orElseThrow(() -> new EntityNotFoundException("Método de pago no encontrado."));
    }

}