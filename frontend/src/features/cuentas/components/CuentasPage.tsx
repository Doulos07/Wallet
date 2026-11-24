import { useState } from "react";
import {
  AlertCircle,
  Banknote,
  Building2,
  CreditCard,
  FileSpreadsheet,
  HandCoins,
  Home,
  Landmark,
  PiggyBank,
  ShieldCheck,
  Trash2,
  TrendingUp,
  Wallet,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { obtenerMensajeError } from "@/lib/errors";
import { formatearCentavos } from "@/lib/money";
import {
  ETIQUETAS_TIPO_CUENTA,
  type CuentaResponseDTO,
  type TipoCuentaUsuario,
} from "@/types/cuentas";
import type { TipoCuenta } from "@/types/enums";
import { useCuentas, useDesactivarCuenta } from "../hooks/useCuentas";
import { CrearCuentaDialog } from "./CrearCuentaDialog";

const ICONOS_TIPO: Record<TipoCuenta, LucideIcon> = {
  CUENTA_GENERAL: Wallet,
  EFECTIVO: Banknote,
  CUENTA_CORRIENTE: Building2,
  TARJETA_CREDITO: CreditCard,
  CUENTA_AHORRO: PiggyBank,
  BONO: FileSpreadsheet,
  SEGURO: ShieldCheck,
  INVERSION: TrendingUp,
  PRESTAMO: HandCoins,
  HIPOTECA: Home,
  CUENTA_SOBREGIRO: AlertCircle,
  EXTERNA: Landmark,
};

export function CuentasPage() {
  const { data: cuentas = [], isLoading, isError, error } = useCuentas();
  const desactivarCuenta = useDesactivarCuenta();
  const [accionError, setAccionError] = useState<string | null>(null);

  const cuentasVisibles = cuentas.filter((c) => c.tipo !== "EXTERNA");

  const saldoTotalCentavos = cuentasVisibles.reduce(
    (acum, cuenta) => acum + cuenta.saldoCentavos,
    0,
  );

  const handleDesactivar = (cuenta: CuentaResponseDTO) => {
    const confirmado = window.confirm(
      `¿Desactivar la cuenta "${cuenta.nombre}"? Ya no aparecerá para nuevos movimientos.`,
    );
    if (!confirmado) return;

    setAccionError(null);
    desactivarCuenta.mutate(cuenta.id, {
      onError: (err) => setAccionError(obtenerMensajeError(err)),
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Mis cuentas</h1>
          <p className="text-sm text-muted-foreground">
            Administrá tus billeteras, bancos y efectivo disponible
          </p>
        </div>
        <CrearCuentaDialog />
      </div>

      <Card className="border-primary/20 bg-primary/5">
        <CardContent className="flex items-center justify-between p-5">
          <div>
            <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
              Saldo total disponible
            </p>
            <p className="mt-1 text-2xl font-bold tracking-tight sm:text-3xl">
              {isLoading
                ? "Cargando..."
                : formatearCentavos(saldoTotalCentavos)}
            </p>
          </div>
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
            <Wallet className="h-6 w-6" />
          </div>
        </CardContent>
      </Card>

      {accionError && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {accionError}
        </div>
      )}

      {isError && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {obtenerMensajeError(error)}
        </div>
      )}

      {!isLoading && !isError && cuentasVisibles.length === 0 && (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12 text-center">
            <Wallet className="h-10 w-10 text-muted-foreground/60" />
            <h2 className="mt-3 text-base font-semibold">
              Todavía no tenés cuentas
            </h2>
            <p className="mt-1 max-w-sm text-sm text-muted-foreground">
              Creá tu primera cuenta de efectivo o banco para empezar a
              registrar gastos e ingresos.
            </p>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        {cuentasVisibles.map((cuenta) => {
          const Icono = ICONOS_TIPO[cuenta.tipo] ?? Wallet;
          const etiquetaTipo =
            cuenta.tipo !== "EXTERNA"
              ? ETIQUETAS_TIPO_CUENTA[cuenta.tipo as TipoCuentaUsuario]
              : "Externa";
          const esNegativo = cuenta.saldoCentavos < 0;

          return (
            <Card key={cuenta.id} className="transition-shadow hover:shadow-sm">
              <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-2">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-muted">
                    <Icono className="h-5 w-5 text-foreground" />
                  </div>
                  <div>
                    <CardTitle className="text-base font-semibold">
                      {cuenta.nombre}
                    </CardTitle>
                    <Badge variant="secondary" className="mt-1 font-normal">
                      {etiquetaTipo}
                    </Badge>
                  </div>
                </div>

                <Button
                  variant="ghost"
                  size="icon"
                  className="h-9 w-9 text-muted-foreground hover:text-destructive"
                  onClick={() => handleDesactivar(cuenta)}
                  disabled={desactivarCuenta.isPending}
                  aria-label={`Desactivar ${cuenta.nombre}`}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </CardHeader>

              <CardContent className="pt-2">
                <p
                  className={`text-xl font-bold tracking-tight ${
                    esNegativo ? "text-destructive" : "text-foreground"
                  }`}
                >
                  {formatearCentavos(cuenta.saldoCentavos)}
                </p>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
