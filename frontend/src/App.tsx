import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "@/components/layout/AppLayout";
import { ProtectedRoute } from "@/components/layout/ProtectedRoute";
import { LoginPage } from "@/features/auth/components/LoginPage";
import { RegisterPage } from "@/features/auth/components/RegisterPage";
import { CatalogosPage } from "@/features/catalogos/components/CatalogosPage";
import { CuentasPage } from "@/features/cuentas/components/CuentasPage";
import { DashboardPage } from "@/features/dashboard/components/DashboardPage";
import { EmpleadosPage } from "@/features/empleados/components/EmpleadosPage";
import { PagosPlanificadosPage } from "@/features/pagos-planificados/components/PagosPlanificadosPage";
import { TransaccionesPage } from "@/features/transacciones/components/TransaccionesPage";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60, // 1 minuto de caché fresca antes de revalidar
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Rutas públicas */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/registro" element={<RegisterPage />} />

          {/* Rutas protegidas por JWT */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/cuentas" element={<CuentasPage />} />
              <Route path="/transacciones" element={<TransaccionesPage />} />
              <Route
                path="/pagos-planificados"
                element={<PagosPlanificadosPage />}
              />
              <Route path="/empleados" element={<EmpleadosPage />} />
              <Route path="/catalogos" element={<CatalogosPage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
