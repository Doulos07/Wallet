import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import type { ErrorResponseDTO } from "@/types/ErrorResponseDTO";

export class ApiError extends Error {
  public readonly status: number;
  public readonly error: string;
  public readonly mensaje: string;
  public readonly path: string;
  public readonly erroresCampos?: Record<string, string>;

  constructor(dto: ErrorResponseDTO) {
    super(dto.mensaje);
    this.name = "ApiError";
    this.status = dto.status;
    this.error = dto.error;
    this.mensaje = dto.mensaje;
    this.path = dto.path;
    this.erroresCampos = dto.erroresCampos;
  }
}

/**
 * Si la respuesta 400 trae `erroresCampos` desde @Valid, inyecta cada mensaje
 * en el campo correspondiente de React Hook Form. Devuelve true si hubo errores de campo.
 */
export function aplicarErroresCampos<T extends FieldValues>(
  err: unknown,
  setError: UseFormSetError<T>,
): boolean {
  if (!(err instanceof ApiError) || !err.erroresCampos) {
    return false;
  }

  const entradas = Object.entries(err.erroresCampos);
  if (entradas.length === 0) {
    return false;
  }

  for (const [campo, mensaje] of entradas) {
    setError(campo as Path<T>, { type: "server", message: mensaje });
  }
  return true;
}

export function obtenerMensajeError(err: unknown): string {
  if (err instanceof ApiError) {
    return err.mensaje;
  }
  if (err instanceof Error) {
    return err.message;
  }
  return "Ocurrió un error inesperado al comunicarse con el servidor.";
}
