import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { pagosPlanificadosApi } from "@/api/pagoPlanificado";
import type {
  CrearPagoPlanificadoDTO,
  PagoPlanificadoResponseDTO,
} from "@/types/pagosPlanificados";

export const PAGOS_PLANIFICADOS_QUERY_KEY = ["pagos-planificados"] as const;

export function usePagosPlanificados() {
  return useQuery<PagoPlanificadoResponseDTO[]>({
    queryKey: PAGOS_PLANIFICADOS_QUERY_KEY,
    queryFn: pagosPlanificadosApi.listarActivos,
  });
}

export function useCrearPagoPlanificado() {
  const queryClient = useQueryClient();

  return useMutation<
    PagoPlanificadoResponseDTO,
    Error,
    CrearPagoPlanificadoDTO
  >({
    mutationFn: (dto: CrearPagoPlanificadoDTO) =>
      pagosPlanificadosApi.crear(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: PAGOS_PLANIFICADOS_QUERY_KEY,
      });
    },
  });
}

export function useDesactivarPagoPlanificado() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, number>({
    mutationFn: (id: number) => pagosPlanificadosApi.desactivar(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: PAGOS_PLANIFICADOS_QUERY_KEY,
      });
    },
  });
}
