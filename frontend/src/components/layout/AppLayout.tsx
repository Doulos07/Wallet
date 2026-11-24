import { NavLink, Outlet } from "react-router-dom";
import {
  ArrowLeftRight,
  CalendarClock,
  LayoutDashboard,
  LogOut,
  Tags,
  Users,
  Wallet,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/hooks/useAuth";

const NAV_ITEMS = [
  { to: "/", label: "Inicio", icon: LayoutDashboard },
  { to: "/cuentas", label: "Cuentas", icon: Wallet },
  { to: "/transacciones", label: "Movimientos", icon: ArrowLeftRight },
  { to: "/pagos-planificados", label: "Agenda", icon: CalendarClock },
  { to: "/empleados", label: "Nómina", icon: Users },
  { to: "/catalogos", label: "Catálogos", icon: Tags },
] as const;

export function AppLayout() {
  const { logout } = useAuth();

  return (
    <div className="flex min-h-dvh flex-col bg-background md:flex-row">
      {/* Barra lateral en escritorio */}
      <aside className="hidden w-60 flex-col justify-between border-r border-border bg-card p-4 md:flex">
        <div className="space-y-6">
          <div className="flex items-center gap-2.5 px-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary text-primary-foreground">
              <Wallet className="h-5 w-5" />
            </div>
            <span className="text-lg font-bold tracking-tight">Wallet</span>
          </div>

          <nav className="space-y-1">
            {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                end={to === "/"}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                    isActive
                      ? "bg-primary text-primary-foreground"
                      : "text-muted-foreground hover:bg-muted hover:text-foreground"
                  }`
                }
              >
                <Icon className="h-4 w-4" />
                {label}
              </NavLink>
            ))}
          </nav>
        </div>

        <Button
          variant="ghost"
          className="justify-start gap-3 text-muted-foreground"
          onClick={logout}
        >
          <LogOut className="h-4 w-4" />
          Cerrar sesión
        </Button>
      </aside>

      {/* Cabecera compacta en celular */}
      <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-border bg-background/95 px-4 backdrop-blur md:hidden">
        <div className="flex items-center gap-2">
          <Wallet className="h-5 w-5 text-primary" />
          <span className="font-bold tracking-tight">Wallet</span>
        </div>
        <Button
          variant="ghost"
          size="sm"
          onClick={logout}
          aria-label="Cerrar sesión"
        >
          <LogOut className="h-4 w-4" />
        </Button>
      </header>

      {/* Contenido principal con espacio inferior en móvil para la barra fija */}
      <main className="flex-1 px-4 pt-4 pb-20 md:p-8">
        <div className="mx-auto max-w-5xl">
          <Outlet />
        </div>
      </main>

      {/* Barra de navegación inferior fija en celular */}
      <nav className="fixed inset-x-0 bottom-0 z-30 flex h-16 items-center justify-around border-t border-border bg-card/95 px-1 backdrop-blur md:hidden">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === "/"}
            className={({ isActive }) =>
              `flex min-w-0 flex-1 flex-col items-center justify-center gap-1 py-1 text-[11px] font-medium transition-colors ${
                isActive ? "text-primary" : "text-muted-foreground"
              }`
            }
          >
            <Icon className="h-5 w-5" />
            <span className="truncate">{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
