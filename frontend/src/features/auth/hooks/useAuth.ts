import { useCallback } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { authApi } from "@/api/auth";
import { TOKEN_STORAGE_KEY } from "@/api/client";
import type { CrearUsuarioDTO, LoginUsuarioDTO } from "@/types/auth";

export function useAuth() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const isAuthenticated = Boolean(localStorage.getItem(TOKEN_STORAGE_KEY));

  const loginMutation = useMutation({
    mutationFn: (dto: LoginUsuarioDTO) => authApi.login(dto),
    onSuccess: ({ token }) => {
      localStorage.setItem(TOKEN_STORAGE_KEY, token);
      navigate("/", { replace: true });
    },
  });

  const registerMutation = useMutation({
    mutationFn: (dto: CrearUsuarioDTO) => authApi.registrar(dto),
    onSuccess: () => {
      navigate("/login?registrado=1", { replace: true });
    },
  });

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    queryClient.clear();
    navigate("/login", { replace: true });
  }, [navigate, queryClient]);

  return {
    isAuthenticated,
    loginMutation,
    registerMutation,
    logout,
  };
}
