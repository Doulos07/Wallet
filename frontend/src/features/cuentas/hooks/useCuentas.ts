import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cuentasApi } from "@/api/cuentas";
import type { CrearCuentaDTO, CuentaResponseDTO } from "@/types/cuentas";

export const CUENTAS_QUERY_KEY = ["cuentas"] as const;

export function useCuentas() {
  return useQuery<CuentaResponseDTO[]>({
    queryKey: CUENTAS_QUERY_KEY,
    queryFn: cuentasApi.listarActivas,
  });
}

export function useCrearCuenta() {
  const queryClient = useQueryClient();

  return useMutation<CuentaResponseDTO, Error, CrearCuentaDTO>({
    mutationFn: (dto: CrearCuentaDTO) => cuentasApi.crear(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: CUENTAS_QUERY_KEY });
    },
  });
}

export function useDesactivarCuenta() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, number>({
    mutationFn: (id: number) => cuentasApi.desactivar(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: CUENTAS_QUERY_KEY });
    },
  });
}
