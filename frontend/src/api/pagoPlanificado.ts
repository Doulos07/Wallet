import { apiFetch } from "./client";
import type {
  CrearPagoPlanificadoDTO,
  PagoPlanificadoResponseDTO,
} from "@/types/pagosPlanificados";

export const pagosPlanificadosApi = {
  listarActivos: (): Promise<PagoPlanificadoResponseDTO[]> =>
    apiFetch<PagoPlanificadoResponseDTO[]>("/pagos-planificados"),

  crear: (dto: CrearPagoPlanificadoDTO): Promise<PagoPlanificadoResponseDTO> =>
    apiFetch<PagoPlanificadoResponseDTO>("/pagos-planificados", {
      method: "POST",
      body: dto,
    }),

  desactivar: (id: number): Promise<void> =>
    apiFetch<void>(`/pagos-planificados/${id}/desactivar`, {
      method: "PATCH",
    }),
};
