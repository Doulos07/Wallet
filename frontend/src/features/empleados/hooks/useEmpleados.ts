import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { empleadosApi } from "@/api/empleados";
import { CUENTAS_QUERY_KEY } from "@/features/cuentas/hooks/useCuentas";
import { PAGOS_PLANIFICADOS_QUERY_KEY } from "@/features/pagos-planificados/hooks/usePagosPlanificados";
import { TRANSACCIONES_QUERY_KEY } from "@/features/transacciones/hooks/useTransacciones";
import type {
  ActualizarEmpleadoDTO,
  CrearAdelantoSueldoDTO,
  CrearEmpleadoDTO,
  CrearRegistroExtraDTO,
  EmpleadoDetalleResponseDTO,
  EmpleadoResponseDTO,
} from "@/types/empleados";

export const EMPLEADOS_QUERY_KEY = ["empleados"] as const;

export function useEmpleados() {
  return useQuery<EmpleadoResponseDTO[]>({
    queryKey: EMPLEADOS_QUERY_KEY,
    queryFn: empleadosApi.listarActivos,
  });
}

export function useCrearEmpleado() {
  const queryClient = useQueryClient();

  return useMutation<EmpleadoResponseDTO, Error, CrearEmpleadoDTO>({
    mutationFn: (dto: CrearEmpleadoDTO) => empleadosApi.crear(dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: EMPLEADOS_QUERY_KEY });
      void queryClient.invalidateQueries({
        queryKey: PAGOS_PLANIFICADOS_QUERY_KEY,
      });
    },
  });
}

export function useEmpleadoDetalle(empleadoId: number | null) {
  return useQuery<EmpleadoDetalleResponseDTO>({
    queryKey: [...EMPLEADOS_QUERY_KEY, empleadoId, "detalle"],
    queryFn: () => empleadosApi.obtenerDetalle(empleadoId!),
    enabled: empleadoId !== null,
  });
}

export function useActualizarCondicionesEmpleado() {
  const queryClient = useQueryClient();

  return useMutation<
    EmpleadoResponseDTO,
    Error,
    { empleadoId: number; dto: ActualizarEmpleadoDTO }
  >({
    mutationFn: ({ empleadoId, dto }) =>
      empleadosApi.actualizarCondiciones(empleadoId, dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: EMPLEADOS_QUERY_KEY });
      void queryClient.invalidateQueries({
        queryKey: PAGOS_PLANIFICADOS_QUERY_KEY,
      });
    },
  });
}

export function useRegistrarExtra() {
  const queryClient = useQueryClient();

  return useMutation<
    void,
    Error,
    { empleadoId: number; dto: CrearRegistroExtraDTO }
  >({
    mutationFn: ({ empleadoId, dto }) =>
      empleadosApi.registrarExtra(empleadoId, dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: EMPLEADOS_QUERY_KEY });
      void queryClient.invalidateQueries({
        queryKey: PAGOS_PLANIFICADOS_QUERY_KEY,
      });
    },
  });
}

export function useRegistrarAdelanto() {
  const queryClient = useQueryClient();

  return useMutation<
    void,
    Error,
    { empleadoId: number; dto: CrearAdelantoSueldoDTO }
  >({
    mutationFn: ({ empleadoId, dto }) =>
      empleadosApi.registrarAdelanto(empleadoId, dto),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: EMPLEADOS_QUERY_KEY });
      void queryClient.invalidateQueries({
        queryKey: PAGOS_PLANIFICADOS_QUERY_KEY,
      });
      void queryClient.invalidateQueries({ queryKey: TRANSACCIONES_QUERY_KEY });
      void queryClient.invalidateQueries({ queryKey: CUENTAS_QUERY_KEY });
    },
  });
}

export function useDesactivarEmpleado() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, number>({
    mutationFn: (empleadoId: number) => empleadosApi.desactivar(empleadoId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: EMPLEADOS_QUERY_KEY });
      void queryClient.invalidateQueries({
        queryKey: PAGOS_PLANIFICADOS_QUERY_KEY,
      });
    },
  });
}
