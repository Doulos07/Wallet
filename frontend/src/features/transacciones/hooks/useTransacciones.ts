import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { transaccionesApi } from "@/api/transacciones";
import { CUENTAS_QUERY_KEY } from "@/features/cuentas/hooks/useCuentas";
import { EMPLEADOS_QUERY_KEY } from "@/features/empleados/hooks/useEmpleados";
import type { PageResponse } from "@/types/common";
import type {
  CrearGastoDTO,
  CrearIngresoDTO,
  CrearTransferenciaDTO,
  FiltrosTransaccion,
  TransaccionResponseDTO,
} from "@/types/transacciones";

export const TRANSACCIONES_QUERY_KEY = ["transacciones"] as const;

export function useTransacciones(filtros: FiltrosTransaccion) {
  return useQuery<PageResponse<TransaccionResponseDTO>>({
    queryKey: [...TRANSACCIONES_QUERY_KEY, filtros],
    queryFn: () => transaccionesApi.listar(filtros),
  });
}

export function useCrearGasto() {
  const queryClient = useQueryClient();

  return useMutation<TransaccionResponseDTO, Error, CrearGastoDTO>({
    mutationFn: (dto: CrearGastoDTO) => transaccionesApi.crearGasto(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TRANSACCIONES_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: CUENTAS_QUERY_KEY });
    },
  });
}

export function useCrearIngreso() {
  const queryClient = useQueryClient();

  return useMutation<TransaccionResponseDTO, Error, CrearIngresoDTO>({
    mutationFn: (dto: CrearIngresoDTO) => transaccionesApi.crearIngreso(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TRANSACCIONES_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: CUENTAS_QUERY_KEY });
    },
  });
}

export function useCrearTransferencia() {
  const queryClient = useQueryClient();

  return useMutation<TransaccionResponseDTO, Error, CrearTransferenciaDTO>({
    mutationFn: (dto: CrearTransferenciaDTO) =>
      transaccionesApi.crearTransferencia(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TRANSACCIONES_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: CUENTAS_QUERY_KEY });
    },
  });
}

export function useAnularTransaccion() {
  const queryClient = useQueryClient();

  return useMutation<TransaccionResponseDTO, Error, number>({
    mutationFn: (id: number) => transaccionesApi.anular(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TRANSACCIONES_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: CUENTAS_QUERY_KEY });
    },
  });
}

export function useAutorizarTransaccion() {
  const queryClient = useQueryClient();

  return useMutation<TransaccionResponseDTO, Error, number>({
    mutationFn: (id: number) => transaccionesApi.autorizar(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TRANSACCIONES_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: CUENTAS_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: EMPLEADOS_QUERY_KEY });
    },
  });
}
