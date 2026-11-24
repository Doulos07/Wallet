const formateadorARS = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** Convierte centavos enteros (Long del backend) a texto formateado en pesos argentinos. */
export function formatearCentavos(centavos: number): string {
  return formateadorARS.format(centavos / 100);
}

/** Convierte un monto decimal ingresado en formularios a centavos enteros sin errores de punto flotante. */
export function pesosACentavos(pesos: number): number {
  return Math.round(pesos * 100);
}

/** Convierte centavos enteros a número decimal para precargar inputs de edición. */
export function centavosAPesos(centavos: number): number {
  return centavos / 100;
}
