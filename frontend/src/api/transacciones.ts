import { apiFetch } from "./client";
import type { PageResponse } from "@/types/common";
import type {
  CrearGastoDTO,
  CrearIngresoDTO,
  CrearTransferenciaDTO,
  FiltrosTransaccion,
  TransaccionResponseDTO,
} from "@/types/transacciones";

function normalizarFechaLocalDateTime(fecha?: string): string | undefined {
  if (!fecha) return undefined;
  if (fecha.includes("T")) return fecha;

  const ahora = new Date();
  const horas = String(ahora.getHours()).padStart(2, "0");
  const minutos = String(ahora.getMinutes()).padStart(2, "0");
  const segundos = String(ahora.getSeconds()).padStart(2, "0");
  return `${fecha}T${horas}:${minutos}:${segundos}`;
}

function construirQueryTransacciones(filtros?: FiltrosTransaccion): string {
  if (!filtros) return "";

  const params = new URLSearchParams();

  if (filtros.cuentaId !== undefined) {
    params.set("cuentaId", String(filtros.cuentaId));
  }
  if (filtros.estadoNegocio !== undefined) {
    params.set("estadoNegocio", filtros.estadoNegocio);
  }
  if (filtros.estadoBancario !== undefined) {
    params.set("estadoBancario", filtros.estadoBancario);
  }
  if (filtros.desde) {
    params.set("desde", filtros.desde);
  }
  if (filtros.hasta) {
    params.set("hasta", filtros.hasta);
  }
  if (filtros.page !== undefined) {
    params.set("page", String(filtros.page));
  }
  if (filtros.size !== undefined) {
    params.set("size", String(filtros.size));
  }
  if (filtros.sort) {
    params.set("sort", filtros.sort);
  }

  const query = params.toString();
  return query ? `?${query}` : "";
}

export const transaccionesApi = {
  listar: (
    filtros?: FiltrosTransaccion,
  ): Promise<PageResponse<TransaccionResponseDTO>> =>
    apiFetch<PageResponse<TransaccionResponseDTO>>(
      `/transacciones${construirQueryTransacciones(filtros)}`,
    ),

  crearGasto: (dto: CrearGastoDTO): Promise<TransaccionResponseDTO> =>
    apiFetch<TransaccionResponseDTO>("/transacciones/gastos", {
      method: "POST",
      body: {
        ...dto,
        fecha: normalizarFechaLocalDateTime(dto.fecha),
        tipoMovimiento: "GASTO" as const,
      },
    }),

  crearIngreso: (dto: CrearIngresoDTO): Promise<TransaccionResponseDTO> =>
    apiFetch<TransaccionResponseDTO>("/transacciones/ingresos", {
      method: "POST",
      body: {
        ...dto,
        fecha: normalizarFechaLocalDateTime(dto.fecha),
        tipoMovimiento: "INGRESO" as const,
      },
    }),

  crearTransferencia: (
    dto: CrearTransferenciaDTO,
  ): Promise<TransaccionResponseDTO> =>
    apiFetch<TransaccionResponseDTO>("/transacciones/transferencias", {
      method: "POST",
      body: {
        ...dto,
        fecha: normalizarFechaLocalDateTime(dto.fecha),
      },
    }),

  anular: (id: number): Promise<TransaccionResponseDTO> =>
    apiFetch<TransaccionResponseDTO>(`/transacciones/${id}/anular`, {
      method: "PATCH",
    }),

  autorizar: (id: number): Promise<TransaccionResponseDTO> =>
    apiFetch<TransaccionResponseDTO>(`/transacciones/${id}/autorizar`, {
      method: "PATCH",
    }),
};
