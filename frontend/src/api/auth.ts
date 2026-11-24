import { apiFetch } from "./client";
import type {
  AuthResponseDTO,
  CrearUsuarioDTO,
  LoginUsuarioDTO,
  UsuarioResponseDTO,
} from "@/types/auth";

export const authApi = {
  login: (dto: LoginUsuarioDTO) =>
    apiFetch<AuthResponseDTO>("/usuarios/login", {
      method: "POST",
      body: dto,
    }),

  registrar: (dto: CrearUsuarioDTO) =>
    apiFetch<UsuarioResponseDTO>("/usuarios/registro", {
      method: "POST",
      body: dto,
    }),
};
