import { z } from "zod";

export const TIPOS_OPERACION_FORM = [
  "GASTO",
  "INGRESO",
  "TRANSFERENCIA",
] as const;

export type TipoOperacionForm = (typeof TIPOS_OPERACION_FORM)[number];

export const nuevaTransaccionSchema = z
  .object({
    tipoMovimiento: z.enum(TIPOS_OPERACION_FORM),
    montoPesos: z
      .number({ message: "Ingresá un monto válido." })
      .positive("El monto debe ser mayor a cero.")
      .max(999_999_999, "El monto supera el límite permitido."),
    cuentaOrigenId: z.number().int().positive("Seleccioná una cuenta."),
    cuentaDestinoId: z.number().int().nonnegative(),
    categoriaId: z.number().int().nonnegative(),
    metodoPagoId: z.number().int().positive("Seleccioná un método de pago."),
    fecha: z.string().optional(),
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
    garantiaMeses: z
      .number()
      .int("Debe ser un número entero.")
      .min(1, "Mínimo 1 mes.")
      .max(120, "Máximo 120 meses.")
      .optional(),
  })
  .superRefine((data, ctx) => {
    if (data.tipoMovimiento === "TRANSFERENCIA") {
      if (data.cuentaDestinoId <= 0) {
        ctx.addIssue({
          code: "custom",
          path: ["cuentaDestinoId"],
          message: "Seleccioná la cuenta de destino.",
        });
      } else if (data.cuentaDestinoId === data.cuentaOrigenId) {
        ctx.addIssue({
          code: "custom",
          path: ["cuentaDestinoId"],
          message: "La cuenta destino debe ser distinta a la de origen.",
        });
      }
    } else if (data.categoriaId <= 0) {
      ctx.addIssue({
        code: "custom",
        path: ["categoriaId"],
        message: "Seleccioná una categoría.",
      });
    }
  });

export type NuevaTransaccionFormValues = z.infer<typeof nuevaTransaccionSchema>;
