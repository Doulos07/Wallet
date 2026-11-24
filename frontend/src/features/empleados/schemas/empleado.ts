import { z } from "zod";

export const TIPOS_PAGO_HORA_EXTRA = [
  "MULTIPLICADOR_HORA",
  "MONTO_FIJO_HORA",
  "POR_TRABAJO_REALIZADO",
] as const;

export const crearEmpleadoSchema = z.object({
  nombre: z
    .string()
    .trim()
    .min(1, "El nombre es obligatorio.")
    .max(100, "Máximo 100 caracteres."),
  apellido: z
    .string()
    .trim()
    .min(1, "El apellido es obligatorio.")
    .max(100, "Máximo 100 caracteres."),
  dni: z
    .string()
    .trim()
    .min(1, "El DNI es obligatorio.")
    .max(20, "Máximo 20 caracteres."),
  sueldoBasePesos: z
    .number({ message: "Ingresá un sueldo válido." })
    .positive("El sueldo base debe ser mayor a cero.")
    .max(999_999_999, "El monto supera el límite permitido."),
  horasMensualesBase: z
    .number({ message: "Ingresá las horas mensuales base." })
    .int("Debe ser un número entero.")
    .positive("Debe ser mayor a cero.")
    .max(744, "Máximo 744 horas mensuales."),
  tipoHoraExtra: z.enum(TIPOS_PAGO_HORA_EXTRA),
  valorHoraExtra: z
    .number({ message: "Ingresá un valor válido." })
    .min(0, "El valor no puede ser negativo."),
  diaPagoSueldo: z
    .number({ message: "Ingresá un día válido." })
    .int("Debe ser entero.")
    .min(1, "Entre 1 y 31.")
    .max(31, "Entre 1 y 31."),
  diaPagoExtras: z
    .number({ message: "Ingresá un día válido." })
    .int("Debe ser entero.")
    .min(1, "Entre 1 y 31.")
    .max(31, "Entre 1 y 31."),
  cuentaOrigenId: z.number().int().positive("Seleccioná la cuenta de pago."),
  categoriaId: z.number().int().positive("Seleccioná una categoría."),
  metodoPagoId: z.number().int().positive("Seleccioná un método de pago."),
});

export type CrearEmpleadoFormValues = z.infer<typeof crearEmpleadoSchema>;

export const actualizarEmpleadoSchema = z.object({
  sueldoBasePesos: z
    .number({ message: "Ingresá un sueldo válido." })
    .positive("El sueldo base debe ser mayor a cero.")
    .max(999_999_999, "El monto supera el límite permitido."),
  horasMensualesBase: z
    .number({ message: "Ingresá las horas mensuales base." })
    .int("Debe ser un número entero.")
    .positive("Debe ser mayor a cero.")
    .max(744, "Máximo 744 horas mensuales."),
  tipoHoraExtra: z.enum(TIPOS_PAGO_HORA_EXTRA),
  valorHoraExtra: z
    .number({ message: "Ingresá un valor válido." })
    .min(0, "El valor no puede ser negativo."),
  diaPagoSueldo: z
    .number({ message: "Ingresá un día válido." })
    .int("Debe ser entero.")
    .min(1, "Entre 1 y 31.")
    .max(31, "Entre 1 y 31."),
  diaPagoExtras: z
    .number({ message: "Ingresá un día válido." })
    .int("Debe ser entero.")
    .min(1, "Entre 1 y 31.")
    .max(31, "Entre 1 y 31."),
});

export type ActualizarEmpleadoFormValues = z.infer<
  typeof actualizarEmpleadoSchema
>;

export const crearRegistroExtraSchema = z
  .object({
    fecha: z.string().min(1, "La fecha es obligatoria."),
    cantidadHorasExtra: z
      .number()
      .positive("Las horas deben ser mayores a cero.")
      .optional(),
    montoManualPesos: z
      .number()
      .positive("El monto debe ser mayor a cero.")
      .optional(),
    descripcion: z
      .string()
      .trim()
      .max(255, "La descripción no puede superar los 255 caracteres.")
      .optional(),
  })
  .superRefine((data, ctx) => {
    const tieneHoras =
      data.cantidadHorasExtra !== undefined &&
      !Number.isNaN(data.cantidadHorasExtra) &&
      data.cantidadHorasExtra > 0;
    const tieneMonto =
      data.montoManualPesos !== undefined &&
      !Number.isNaN(data.montoManualPesos) &&
      data.montoManualPesos > 0;

    if (!tieneHoras && !tieneMonto) {
      ctx.addIssue({
        code: "custom",
        path: ["cantidadHorasExtra"],
        message: "Ingresá la cantidad de horas o un monto manual.",
      });
    }
  });

export type CrearRegistroExtraFormValues = z.infer<
  typeof crearRegistroExtraSchema
>;

export const crearAdelantoSueldoSchema = z.object({
  fecha: z.string().min(1, "La fecha es obligatoria."),
  montoPesos: z
    .number({ message: "Ingresá un monto válido." })
    .positive("El monto debe ser mayor a cero.")
    .max(999_999_999, "El monto supera el límite permitido."),
  motivo: z
    .string()
    .trim()
    .max(255, "El motivo no puede superar los 255 caracteres.")
    .optional(),
  cuentaOrigenId: z.number().int().positive("Seleccioná la cuenta de origen."),
  categoriaId: z.number().int().positive("Seleccioná una categoría."),
  metodoPagoId: z.number().int().positive("Seleccioná un método de pago."),
});

export type CrearAdelantoSueldoFormValues = z.infer<
  typeof crearAdelantoSueldoSchema
>;
