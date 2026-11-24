import type { TipoCuenta } from "./enums";

export type TipoCuentaUsuario = Exclude<TipoCuenta, "EXTERNA">;

export const ETIQUETAS_TIPO_CUENTA: Record<TipoCuentaUsuario, string> = {
  CUENTA_GENERAL: "Cuenta general",
  EFECTIVO: "Efectivo",
  CUENTA_CORRIENTE: "Cuenta corriente",
  CUENTA_AHORRO: "Caja de ahorro",
  TARJETA_CREDITO: "Tarjeta de crédito",
  BONO: "Bono",
  SEGURO: "Seguro",
  INVERSION: "Inversión",
  PRESTAMO: "Préstamo",
  HIPOTECA: "Hipoteca",
  CUENTA_SOBREGIRO: "Cuenta sobregiro",
};

export interface CrearCuentaDTO {
  nombre: string;
  tipo: TipoCuenta;
  montoInicialCentavos: number;
}

export interface CuentaResponseDTO {
  id: number;
  nombre: string;
  tipo: TipoCuenta;
  saldoCentavos: number;
  estado: boolean;
}
