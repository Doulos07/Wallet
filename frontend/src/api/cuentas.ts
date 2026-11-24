import { apiFetch } from "./client";
import type { CrearCuentaDTO, CuentaResponseDTO } from "@/types/cuentas";

export const cuentasApi = {
  listarActivas: (): Promise<CuentaResponseDTO[]> =>
    apiFetch<CuentaResponseDTO[]>("/cuentas"),

  crear: (dto: CrearCuentaDTO): Promise<CuentaResponseDTO> =>
    apiFetch<CuentaResponseDTO>("/cuentas/registrar", {
      method: "POST",
      body: dto,
    }),

  desactivar: (id: number): Promise<void> =>
    apiFetch<void>(`/cuentas/${id}/desactivar`, {
      method: "PATCH",
    }),
};
