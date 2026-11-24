export interface ErrorResponseDTO {
  timestamp: string;
  status: number;
  error: string;
  mensaje: string;
  path: string;
  erroresCampos?: Record<string, string>;
}
