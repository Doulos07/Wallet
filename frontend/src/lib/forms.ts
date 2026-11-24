// Convierte valores vacíos o inválidos de inputs numéricos opcionales en undefined
export function parsearNumeroOpcional(val: unknown): number | undefined {
  if (val === "" || val === null || val === undefined) return undefined;
  const num = Number(val);
  return Number.isFinite(num) ? num : undefined;
}
