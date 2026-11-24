import type { FrecuenciaPago, TipoRecurrenciaPago } from "./enums";
import type {
  TransaccionPlanificableDTO,
  TransaccionResponseDTO,
} from "./transacciones";

interface CrearPagoPlanificadoBaseDTO {
  fecha: string; // LocalDate: YYYY-MM-DD
  plantilla: TransaccionPlanificableDTO;
}

export interface CrearPagoUnicoDTO extends CrearPagoPlanificadoBaseDTO {
  tipoRecurrencia: "UNICO";
}

export interface CrearPagoInfinitoDTO extends CrearPagoPlanificadoBaseDTO {
  tipoRecurrencia: "INFINITO";
  frecuencia: FrecuenciaPago;
}

export interface CrearPagoHastaFechaDTO extends CrearPagoPlanificadoBaseDTO {
  tipoRecurrencia: "HASTA_FECHA";
  frecuencia: FrecuenciaPago;
  fechaFin: string; // LocalDate: YYYY-MM-DD
}

export interface CrearPagoPorEventoDTO extends CrearPagoPlanificadoBaseDTO {
  tipoRecurrencia: "POR_EVENTO";
  frecuencia: FrecuenciaPago;
  cantidadEventos: number;
}

export type CrearPagoPlanificadoDTO =
  | CrearPagoUnicoDTO
  | CrearPagoInfinitoDTO
  | CrearPagoHastaFechaDTO
  | CrearPagoPorEventoDTO;

export interface PagoPlanificadoResponseDTO {
  id: number;
  tipoRecurrencia: TipoRecurrenciaPago;
  fecha: string;
  activo: boolean;
  frecuencia: FrecuenciaPago | null;
  fechaFin: string | null;
  cantidadEventos: number | null;
  eventoActual: number | null;
  transaccion: TransaccionResponseDTO | null;
}
