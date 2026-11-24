import { z } from "zod";

export const crearCatalogoSchema = z.object({
  nombre: z
    .string()
    .trim()
    .min(1, "El nombre es obligatorio.")
    .max(80, "El nombre no puede superar los 80 caracteres."),
});

export type CrearCatalogoFormValues = z.infer<typeof crearCatalogoSchema>;
