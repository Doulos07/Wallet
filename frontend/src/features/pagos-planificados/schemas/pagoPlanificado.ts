import { z } from "zod";

export const TIPOS_RECURRENCIA = [
  "UNICO",
  "INFINITO",
  "HASTA_FECHA",
  "POR_EVENTO",
] as const;

export const FRECUENCIAS_PAGO = ["DAYS", "WEEKS", "MONTHS", "YEARS"] as const;

export const TIPOS_PLANTILLA_PAGO = ["GASTO", "INGRESO"] as const;

function obtenerHoyLocalISO(): string {
  const hoy = new Date();
  const anio = hoy.getFullYear();
  const mes = String(hoy.getMonth() + 1).padStart(2, "0");
  const dia = String(hoy.getDate()).padStart(2, "0");
  return `${anio}-${mes}-${dia}`;
}

export const nuevoPagoPlanificadoSchema = z
  .object({
    tipoMovimiento: z.enum(TIPOS_PLANTILLA_PAGO),
    tipoRecurrencia: z.enum(TIPOS_RECURRENCIA),
    fecha: z.string().min(1, "La fecha de inicio/ejecución es obligatoria."),
    frecuencia: z.enum(FRECUENCIAS_PAGO),
    fechaFin: z.string().optional(),
    cantidadEventos: z.number().int("Debe ser un número entero.").optional(),
    montoPesos: z
      .number({ message: "Ingresá un monto válido." })
      .positive("El monto debe ser mayor a cero.")
      .max(999_999_999, "El monto supera el límite permitido."),
    cuentaOrigenId: z.number().int().positive("Seleccioná una cuenta."),
    categoriaId: z.number().int().positive("Seleccioná una categoría."),
    metodoPagoId: z.number().int().positive("Seleccioná un método de pago."),
    descripcion: z
      .string()
      .trim()
      .max(255, "La descripción no puede superar los 255 caracteres.")
      .optional(),
    tercero: z
      .string()
      .trim()
      .max(120, "El destinatario/comercio no puede superar los 120 caracteres.")
      .optional(),
  })
  .superRefine((data, ctx) => {
    if (data.tipoRecurrencia === "HASTA_FECHA") {
      if (!data.fechaFin) {
        ctx.addIssue({
          code: "custom",
          path: ["fechaFin"],
          message: "La fecha de fin es obligatoria.",
        });
      } else {
        const hoy = obtenerHoyLocalISO();
        if (data.fechaFin <= hoy) {
          ctx.addIssue({
            code: "custom",
            path: ["fechaFin"],
            message: "La fecha de fin debe ser posterior a hoy.",
          });
        }
        if (data.fecha && data.fechaFin < data.fecha) {
          ctx.addIssue({
            code: "custom",
            path: ["fechaFin"],
            message:
              "La fecha de fin debe ser igual o posterior a la fecha de inicio.",
          });
        }
      }
    }

    if (data.tipoRecurrencia === "POR_EVENTO") {
      if (
        data.cantidadEventos === undefined ||
        Number.isNaN(data.cantidadEventos) ||
        data.cantidadEventos < 1
      ) {
        ctx.addIssue({
          code: "custom",
          path: ["cantidadEventos"],
          message: "Ingresá una cantidad de repeticiones mayor o igual a 1.",
        });
      }
    }
  });

export type NuevoPagoPlanificadoFormValues = z.infer<
  typeof nuevoPagoPlanificadoSchema
>;
