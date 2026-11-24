import type { EstadoConciliacion, TipoPagoHoraExtra } from "./enums";

// Condiciones laborales base compartidas entre alta, edición y respuesta
interface EmpleadoBase {
  sueldoBaseCentavos: number;
  horasMensualesBase: number;
  tipoHoraExtra: TipoPagoHoraExtra;
}

// Datos identificatorios del empleado
interface DatosPersonalesEmpleado {
  nombre: string;
  apellido: string;
  dni: string;
}

// Parámetros de imputación contable para sueldos y adelantos
interface ImputacionContableEmpleado {
  cuentaOrigenId: number;
  categoriaId: number;
  metodoPagoId: number;
}

// Condiciones editables de liquidación mensual
interface ConfiguracionLiquidacionEmpleado extends EmpleadoBase {
  diaPagoSueldo?: number;
  diaPagoExtras?: number;
}

export interface EmpleadoResponseDTO
  extends EmpleadoBase, DatosPersonalesEmpleado {
  id: number;
  valorHoraExtra: number;
  diaPagoSueldo: number;
  diaPagoExtras: number;
  activo: boolean;
  proximoPagoSueldo: string | null;
}

export interface CrearEmpleadoDTO
  extends
    ConfiguracionLiquidacionEmpleado,
    DatosPersonalesEmpleado,
    ImputacionContableEmpleado {
  valorHoraExtra?: number;
}

export interface ActualizarEmpleadoDTO extends ConfiguracionLiquidacionEmpleado {
  valorHoraExtra: number;
}

export interface CrearRegistroExtraDTO {
  fecha: string; // LocalDate: YYYY-MM-DD
  cantidadHorasExtra?: number | null;
  descripcion?: string;
  montoManualCentavos?: number | null;
}

export interface CrearAdelantoSueldoDTO extends ImputacionContableEmpleado {
  fecha: string; // LocalDate: YYYY-MM-DD
  montoCentavos: number;
  motivo?: string;
}

export interface RegistroExtraResponseDTO {
  id: number;
  fecha: string;
  cantidadHorasExtra: number | null;
  descripcion: string | null;
  montoCalculadoCentavos: number;
  estadoLiquidacion: EstadoConciliacion;
}

export interface AdelantoSueldoResponseDTO {
  id: number;
  fecha: string;
  montoCentavos: number;
  motivo: string | null;
  descontado: boolean;
}

export interface EmpleadoDetalleResponseDTO extends EmpleadoResponseDTO {
  proximoPagoExtras: string | null;
  totalAdelantosPendientesCentavos: number;
  totalExtrasPendientesCentavos: number;
  adelantos: AdelantoSueldoResponseDTO[];
  registrosExtra: RegistroExtraResponseDTO[];
}
