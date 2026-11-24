export interface LoginUsuarioDTO {
  email: string;
  password: string;
}

export interface CrearUsuarioDTO {
  nombre: string;
  email: string;
  password: string;
}

export interface AuthResponseDTO {
  token: string;
}

export interface UsuarioResponseDTO {
  id: number;
  nombre: string;
  email: string;
}
