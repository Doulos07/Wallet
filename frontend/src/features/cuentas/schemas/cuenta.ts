import { z } from "zod";
import type { TipoCuentaUsuario } from "@/types/cuentas";

const TIPOS_CUENTA_VALIDOS = [
  "CUENTA_GENERAL",
  "EFECTIVO",
  "CUENTA_CORRIENTE",
  "TARJETA_CREDITO",
  "CUENTA_AHORRO",
  "BONO",
  "SEGURO",
  "INVERSION",
  "PRESTAMO",
  "HIPOTECA",
  "CUENTA_SOBREGIRO",
] as const satisfies readonly TipoCuentaUsuario[];

export const crearCuentaSchema = z.object({
  nombre: z
    .string()
    .trim()
    .min(1, "El nombre de la cuenta es obligatorio.")
    .max(60, "El nombre no puede superar los 60 caracteres."),
  tipo: z.enum(TIPOS_CUENTA_VALIDOS, {
    message: "Seleccioná un tipo de cuenta.",
  }),
  montoInicialPesos: z
    .number({ message: "Ingresá un monto numérico válido." })
    .min(0, "El saldo inicial no puede ser negativo."),
});

export type CrearCuentaFormValues = z.infer<typeof crearCuentaSchema>;
