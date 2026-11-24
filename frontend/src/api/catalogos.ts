import { apiFetch } from "./client";
import type {
  CategoriaResponseDTO,
  CrearCategoriaDTO,
  CrearMetodoPagoDTO,
  MetodoPagoResponseDTO,
} from "@/types/catalogos";

export const categoriasApi = {
  listarActivas: (): Promise<CategoriaResponseDTO[]> =>
    apiFetch<CategoriaResponseDTO[]>("/categorias"),

  crear: (dto: CrearCategoriaDTO): Promise<CategoriaResponseDTO> =>
    apiFetch<CategoriaResponseDTO>("/categorias", {
      method: "POST",
      body: dto,
    }),

  desactivar: (id: number): Promise<void> =>
    apiFetch<void>(`/categorias/${id}/desactivar`, {
      method: "PATCH",
    }),
};

export const metodosPagoApi = {
  listarActivos: (): Promise<MetodoPagoResponseDTO[]> =>
    apiFetch<MetodoPagoResponseDTO[]>("/metodos-pago"),

  crear: (dto: CrearMetodoPagoDTO): Promise<MetodoPagoResponseDTO> =>
    apiFetch<MetodoPagoResponseDTO>("/metodos-pago", {
      method: "POST",
      body: dto,
    }),

  desactivar: (id: number): Promise<void> =>
    apiFetch<void>(`/metodos-pago/${id}/desactivar`, {
      method: "PATCH",
    }),
};
