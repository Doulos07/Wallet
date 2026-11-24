import { ApiError } from "@/lib/errors";
import type { ErrorResponseDTO } from "@/types/ErrorResponseDTO";

const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api";
export const TOKEN_STORAGE_KEY = "wallet_jwt_token";

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  params?: Record<string, string | number | boolean | undefined | null>;
}

function construirUrl(
  endpoint: string,
  params?: RequestOptions["params"],
): string {
  const url = new URL(`${BASE_URL}${endpoint}`);
  if (params) {
    for (const [clave, valor] of Object.entries(params)) {
      if (valor !== undefined && valor !== null && valor !== "") {
        url.searchParams.set(clave, String(valor));
      }
    }
  }
  return url.toString();
}

export async function apiFetch<T>(
  endpoint: string,
  options: RequestOptions = {},
): Promise<T> {
  const { body, params, headers, ...rest } = options;
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);

  const requestHeaders = new Headers(headers);
  if (body !== undefined && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }
  if (token) {
    requestHeaders.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(construirUrl(endpoint, params), {
    ...rest,
    headers: requestHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    // Limpia el token local si venció o fue rechazado
    if (response.status === 401 && token) {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }

    let errorDto: ErrorResponseDTO;
    try {
      errorDto = (await response.json()) as ErrorResponseDTO;
    } catch {
      errorDto = {
        timestamp: new Date().toISOString(),
        status: response.status,
        error: response.statusText,
        mensaje: "Error de comunicación con el servidor.",
        path: endpoint,
      };
    }
    throw new ApiError(errorDto);
  }

  // Soporta 204 No Content y 200 OK con cuerpo vacío
  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}
