import type { PageParams } from "./common";
import type {
  EstadoConciliacion,
  EstadoTransaccion,
  TipoMovimientoTransaccion,
} from "./enums";

export interface TransaccionBaseDTO {
  montoCentavos: number;
  descripcion?: string;
  fecha?: string;
  tercero?: string;
  garantiaMeses?: number;
  estadoConciliacion?: EstadoConciliacion;
  estadoNegocio?: EstadoTransaccion;
  cuentaOrigenId: number;
  metodoPagoId: number;
}

export interface CrearGastoDTO extends TransaccionBaseDTO {
  categoriaId: number;
}

export interface CrearIngresoDTO extends TransaccionBaseDTO {
  categoriaId: number;
}

export interface CrearTransferenciaDTO extends TransaccionBaseDTO {
  cuentaDestinoId: number;
}

// Unión discriminada espejo de TransaccionPlanificableDTO (@JsonTypeInfo)
export type TransaccionPlanificableDTO =
  | (CrearGastoDTO & { tipoMovimiento: "GASTO" })
  | (CrearIngresoDTO & { tipoMovimiento: "INGRESO" });

export interface TransaccionResponseDTO {
  id: number;
  tipoMovimiento: TipoMovimientoTransaccion;
  montoCentavos: number;
  descripcion: string | null;
  fecha: string;
  tercero: string | null;
  garantiaMeses: number | null;
  estadoBancario: EstadoConciliacion;
  estadoNegocio: EstadoTransaccion;
  cuentaOrigenId: number;
  cuentaOrigenNombre: string;
  cuentaDestinoId: number | null;
  cuentaDestinoNombre: string | null;
  categoriaId: number | null;
  metodoPagoId: number | null;
}

export interface FiltrosTransaccion extends PageParams {
  cuentaId?: number;
  estadoNegocio?: EstadoTransaccion;
  estadoBancario?: EstadoConciliacion;
  desde?: string;
  hasta?: string;
}
