export interface CategoriaResponseDTO {
  id: number;
  nombre: string;
  estado: boolean;
}

export interface CrearCategoriaDTO {
  nombre: string;
}

export interface MetodoPagoResponseDTO {
  id: number;
  nombre: string;
  estado: boolean;
}

export interface CrearMetodoPagoDTO {
  nombre: string;
}
