export type TipoCuenta =
  | "CUENTA_GENERAL"
  | "EFECTIVO"
  | "CUENTA_CORRIENTE"
  | "TARJETA_CREDITO"
  | "CUENTA_AHORRO"
  | "BONO"
  | "SEGURO"
  | "INVERSION"
  | "PRESTAMO"
  | "HIPOTECA"
  | "CUENTA_SOBREGIRO"
  | "EXTERNA";

export type EstadoTransaccion = "VIGENTE" | "ANULADO";

export type EstadoConciliacion = "CONCILIADO" | "LIQUIDADO" | "NO_LIQUIDADO";

export type TipoMovimeinto = "DEBITO" | "CREDITO";

export type TipoMovimientoTransaccion = "GASTO" | "INGRESO" | "TRANSFERENCIA";

export type TipoPagoHoraExtra =
  | "MULTIPLICADOR_HORA"
  | "MONTO_FIJO_HORA"
  | "POR_TRABAJO_REALIZADO";

export type ModoVista = "NORMAL" | "EXHAUSTIVO";

export type TipoRecurrenciaPago =
  | "UNICO"
  | "INFINITO"
  | "HASTA_FECHA"
  | "POR_EVENTO";

export type FrecuenciaPago = "DAYS" | "WEEKS" | "MONTHS" | "YEARS";
