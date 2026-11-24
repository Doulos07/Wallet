import { Navigate, Outlet } from "react-router-dom";
import { TOKEN_STORAGE_KEY } from "@/api/client";

export function ProtectedRoute() {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
