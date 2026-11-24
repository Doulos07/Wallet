import { apiFetch } from "./client";
import type {
  ActualizarEmpleadoDTO,
  CrearAdelantoSueldoDTO,
  CrearEmpleadoDTO,
  CrearRegistroExtraDTO,
  EmpleadoDetalleResponseDTO,
  EmpleadoResponseDTO,
} from "@/types/empleados";

export const empleadosApi = {
  listarActivos: (): Promise<EmpleadoResponseDTO[]> =>
    apiFetch<EmpleadoResponseDTO[]>("/empleados"),

  obtenerPorId: (empleadoId: number): Promise<EmpleadoResponseDTO> =>
    apiFetch<EmpleadoResponseDTO>(`/empleados/${empleadoId}`),

  crear: (dto: CrearEmpleadoDTO): Promise<EmpleadoResponseDTO> =>
    apiFetch<EmpleadoResponseDTO>("/empleados", {
      method: "POST",
      body: dto,
    }),

  obtenerDetalle: (empleadoId: number): Promise<EmpleadoDetalleResponseDTO> =>
    apiFetch<EmpleadoDetalleResponseDTO>(`/empleados/${empleadoId}/detalle`),

  actualizarCondiciones: (
    empleadoId: number,
    dto: ActualizarEmpleadoDTO,
  ): Promise<EmpleadoResponseDTO> =>
    apiFetch<EmpleadoResponseDTO>(`/empleados/${empleadoId}/condiciones`, {
      method: "PATCH",
      body: dto,
    }),

  registrarExtra: (
    empleadoId: number,
    dto: CrearRegistroExtraDTO,
  ): Promise<void> =>
    apiFetch<void>(`/empleados/${empleadoId}/extras`, {
      method: "POST",
      body: dto,
    }),

  registrarAdelanto: (
    empleadoId: number,
    dto: CrearAdelantoSueldoDTO,
  ): Promise<void> =>
    apiFetch<void>(`/empleados/${empleadoId}/adelantos`, {
      method: "POST",
      body: dto,
    }),

  desactivar: (empleadoId: number): Promise<void> =>
    apiFetch<void>(`/empleados/${empleadoId}/desactivar`, {
      method: "PATCH",
    }),
};
