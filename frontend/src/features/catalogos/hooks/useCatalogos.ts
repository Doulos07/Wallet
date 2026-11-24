import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { categoriasApi, metodosPagoApi } from "@/api/catalogos";
import type {
  CategoriaResponseDTO,
  CrearCategoriaDTO,
  CrearMetodoPagoDTO,
  MetodoPagoResponseDTO,
} from "@/types/catalogos";

export const CATEGORIAS_QUERY_KEY = ["categorias"] as const;
export const METODOS_PAGO_QUERY_KEY = ["metodos-pago"] as const;

export function useCategorias() {
  return useQuery<CategoriaResponseDTO[]>({
    queryKey: CATEGORIAS_QUERY_KEY,
    queryFn: categoriasApi.listarActivas,
  });
}

export function useCrearCategoria() {
  const queryClient = useQueryClient();

  return useMutation<CategoriaResponseDTO, Error, CrearCategoriaDTO>({
    mutationFn: (dto: CrearCategoriaDTO) => categoriasApi.crear(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: CATEGORIAS_QUERY_KEY });
    },
  });
}

export function useDesactivarCategoria() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, number>({
    mutationFn: (id: number) => categoriasApi.desactivar(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: CATEGORIAS_QUERY_KEY });
    },
  });
}

export function useMetodosPago() {
  return useQuery<MetodoPagoResponseDTO[]>({
    queryKey: METODOS_PAGO_QUERY_KEY,
    queryFn: metodosPagoApi.listarActivos,
  });
}

export function useCrearMetodoPago() {
  const queryClient = useQueryClient();

  return useMutation<MetodoPagoResponseDTO, Error, CrearMetodoPagoDTO>({
    mutationFn: (dto: CrearMetodoPagoDTO) => metodosPagoApi.crear(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: METODOS_PAGO_QUERY_KEY });
    },
  });
}

export function useDesactivarMetodoPago() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, number>({
    mutationFn: (id: number) => metodosPagoApi.desactivar(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: METODOS_PAGO_QUERY_KEY });
    },
  });
}
