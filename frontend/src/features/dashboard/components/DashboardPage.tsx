import { useMemo } from "react";
import { Link } from "react-router-dom";
import {
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowRight,
  ArrowUpRight,
  CalendarClock,
  Landmark,
  TrendingDown,
  TrendingUp,
  Users,
  Wallet,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useCategorias } from "@/features/catalogos/hooks/useCatalogos";
import { useCuentas } from "@/features/cuentas/hooks/useCuentas";
import { useEmpleados } from "@/features/empleados/hooks/useEmpleados";
import { usePagosPlanificados } from "@/features/pagos-planificados/hooks/usePagosPlanificados";
import { NuevaTransaccionDialog } from "@/features/transacciones/components/NuevaTransaccionDialog";
import { useTransacciones } from "@/features/transacciones/hooks/useTransacciones";
import { obtenerMensajeError } from "@/lib/errors";

const formateadorARS = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  minimumFractionDigits: 2,
});

function formatearCentavos(centavos: number): string {
  return formateadorARS.format(centavos / 100);
}

function formatearFechaCorta(fechaISO: string): string {
  const soloFecha = fechaISO.slice(0, 10);
  const partes = soloFecha.split("-");
  if (partes.length !== 3) return fechaISO;
  return `${partes[2]}/${partes[1]}/${partes[0]}`;
}

function obtenerRangoMesActual(): {
  desde: string;
  hasta: string;
  etiquetaMes: string;
} {
  const hoy = new Date();
  const anio = hoy.getFullYear();
  const mesIndex = hoy.getMonth();
  const mes = String(mesIndex + 1).padStart(2, "0");
  const ultimoDia = new Date(anio, mesIndex + 1, 0).getDate();

  const etiquetaCruda = new Intl.DateTimeFormat("es-AR", {
    month: "long",
    year: "numeric",
  }).format(hoy);

  return {
    desde: `${anio}-${mes}-01`,
    hasta: `${anio}-${mes}-${String(ultimoDia).padStart(2, "0")}`,
    etiquetaMes: etiquetaCruda.charAt(0).toUpperCase() + etiquetaCruda.slice(1),
  };
}

export function DashboardPage() {
  const { desde, hasta, etiquetaMes } = useMemo(
    () => obtenerRangoMesActual(),
    [],
  );

  const cuentasQuery = useCuentas();
  const categoriasQuery = useCategorias();
  const pagosQuery = usePagosPlanificados();
  const empleadosQuery = useEmpleados();
  const transaccionesMesQuery = useTransacciones({
    desde,
    hasta,
    estadoNegocio: "VIGENTE",
    page: 0,
    size: 200,
  });

  const cuentas = useMemo(
    () =>
      Array.isArray(cuentasQuery.data)
        ? cuentasQuery.data.filter((c) => c.tipo !== "EXTERNA")
        : [],
    [cuentasQuery.data],
  );

  const mapaCategorias = useMemo(() => {
    const lista = Array.isArray(categoriasQuery.data)
      ? categoriasQuery.data
      : [];
    return new Map<number, string>(lista.map((c) => [c.id, c.nombre]));
  }, [categoriasQuery.data]);

  const movimientosMes = useMemo(
    () => transaccionesMesQuery.data?.content ?? [],
    [transaccionesMesQuery.data],
  );

  const pagosProximos = useMemo(() => {
    const lista = Array.isArray(pagosQuery.data) ? pagosQuery.data : [];
    return [...lista]
      .sort((a, b) => a.fecha.localeCompare(b.fecha))
      .slice(0, 5);
  }, [pagosQuery.data]);

  const empleados = useMemo(
    () => (Array.isArray(empleadosQuery.data) ? empleadosQuery.data : []),
    [empleadosQuery.data],
  );

  // Calcula saldos totales, ingresos, gastos y distribución por categoría
  const resumenFinanciero = useMemo(() => {
    const saldoTotalCentavos = cuentas.reduce(
      (acc, c) => acc + c.saldoCentavos,
      0,
    );

    let ingresosMesCentavos = 0;
    let gastosMesCentavos = 0;
    const acumuladoPorCategoria = new Map<number, number>();

    for (const tx of movimientosMes) {
      if (tx.estadoBancario === "NO_LIQUIDADO") continue;
      if (tx.tipoMovimiento === "INGRESO") {
        ingresosMesCentavos += tx.montoCentavos;
      } else if (tx.tipoMovimiento === "GASTO") {
        gastosMesCentavos += tx.montoCentavos;
        if (tx.categoriaId !== null) {
          const previo = acumuladoPorCategoria.get(tx.categoriaId) ?? 0;
          acumuladoPorCategoria.set(tx.categoriaId, previo + tx.montoCentavos);
        }
      }
    }

    const gastosPorCategoria = Array.from(acumuladoPorCategoria.entries())
      .map(([categoriaId, montoCentavos]) => ({
        categoriaId,
        nombre: mapaCategorias.get(categoriaId) ?? "Sin categoría",
        montoCentavos,
        porcentaje:
          gastosMesCentavos > 0
            ? Math.round((montoCentavos / gastosMesCentavos) * 100)
            : 0,
      }))
      .sort((a, b) => b.montoCentavos - a.montoCentavos);

    const masaSalarialCentavos = empleados.reduce(
      (acc, emp) => acc + emp.sueldoBaseCentavos,
      0,
    );

    return {
      saldoTotalCentavos,
      ingresosMesCentavos,
      gastosMesCentavos,
      balanceMesCentavos: ingresosMesCentavos - gastosMesCentavos,
      gastosPorCategoria,
      masaSalarialCentavos,
    };
  }, [cuentas, movimientosMes, mapaCategorias, empleados]);

  const ultimosMovimientos = movimientosMes.slice(0, 5);

  const primerError =
    cuentasQuery.error ??
    transaccionesMesQuery.error ??
    pagosQuery.error ??
    empleadosQuery.error;

  return (
    <div className="space-y-6">
      {/* Cabecera y acción rápida */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Resumen</h1>
          <p className="text-sm text-muted-foreground">
            Estado general de tus cuentas y movimientos de {etiquetaMes}
          </p>
        </div>
        <NuevaTransaccionDialog />
      </div>

      {primerError && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {obtenerMensajeError(primerError)}
        </div>
      )}

      {/* Indicadores principales (KPIs) */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Saldo disponible
            </CardTitle>
            <Wallet className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold tabular-nums">
              {cuentasQuery.isLoading
                ? "..."
                : formatearCentavos(resumenFinanciero.saldoTotalCentavos)}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              En {cuentas.length}{" "}
              {cuentas.length === 1 ? "cuenta activa" : "cuentas activas"}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Ingresos del mes
            </CardTitle>
            <TrendingUp className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold tabular-nums text-emerald-600 dark:text-emerald-400">
              {transaccionesMesQuery.isLoading
                ? "..."
                : `+${formatearCentavos(resumenFinanciero.ingresosMesCentavos)}`}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">{etiquetaMes}</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Gastos del mes
            </CardTitle>
            <TrendingDown className="h-4 w-4 text-destructive" />
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold tabular-nums text-destructive">
              {transaccionesMesQuery.isLoading
                ? "..."
                : `-${formatearCentavos(resumenFinanciero.gastosMesCentavos)}`}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">{etiquetaMes}</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Balance mensual
            </CardTitle>
            <Landmark className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <p
              className={`text-2xl font-bold tabular-nums ${
                resumenFinanciero.balanceMesCentavos >= 0
                  ? "text-emerald-600 dark:text-emerald-400"
                  : "text-destructive"
              }`}
            >
              {transaccionesMesQuery.isLoading
                ? "..."
                : formatearCentavos(resumenFinanciero.balanceMesCentavos)}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              Ingresos menos gastos vigentes
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Bloques operativos */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Columna izquierda: Cuentas y Gastos por categoría */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <CardTitle className="text-base">Cuentas activas</CardTitle>
              <Button variant="ghost" size="sm" className="h-8 gap-1">
                <Link to="/cuentas">
                  Ver todas
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </Button>
            </CardHeader>
            <CardContent>
              {cuentasQuery.isLoading ? (
                <p className="py-4 text-center text-sm text-muted-foreground">
                  Cargando cuentas...
                </p>
              ) : cuentas.length === 0 ? (
                <p className="rounded-lg border border-dashed py-6 text-center text-sm text-muted-foreground">
                  Todavía no creaste ninguna cuenta.
                </p>
              ) : (
                <ul className="divide-y">
                  {cuentas.map((cuenta) => (
                    <li
                      key={cuenta.id}
                      className="flex items-center justify-between py-2.5 first:pt-0 last:pb-0"
                    >
                      <div>
                        <p className="text-sm font-medium">{cuenta.nombre}</p>
                        <p className="text-xs text-muted-foreground">
                          {cuenta.tipo.replaceAll("_", " ")}
                        </p>
                      </div>
                      <span
                        className={`text-sm font-semibold tabular-nums ${
                          cuenta.saldoCentavos < 0
                            ? "text-destructive"
                            : "text-foreground"
                        }`}
                      >
                        {formatearCentavos(cuenta.saldoCentavos)}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base">
                Gastos por categoría ({etiquetaMes})
              </CardTitle>
            </CardHeader>
            <CardContent>
              {transaccionesMesQuery.isLoading ? (
                <p className="py-4 text-center text-sm text-muted-foreground">
                  Calculando distribución...
                </p>
              ) : resumenFinanciero.gastosPorCategoria.length === 0 ? (
                <p className="rounded-lg border border-dashed py-6 text-center text-sm text-muted-foreground">
                  Sin gastos registrados este mes.
                </p>
              ) : (
                <div className="space-y-3.5">
                  {resumenFinanciero.gastosPorCategoria.map((item) => (
                    <div key={item.categoriaId} className="space-y-1.5">
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-medium">{item.nombre}</span>
                        <span className="tabular-nums text-muted-foreground">
                          {formatearCentavos(item.montoCentavos)} (
                          {item.porcentaje}%)
                        </span>
                      </div>
                      <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                        <div
                          className="h-full rounded-full bg-primary transition-all"
                          style={{
                            width: `${Math.min(100, item.porcentaje)}%`,
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Columna derecha: Últimos movimientos, Agenda y Nómina */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <CardTitle className="text-base">Últimos movimientos</CardTitle>
              <Button variant="ghost" size="sm" className="h-8 gap-1">
                <Link to="/transacciones">
                  Historial
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </Button>
            </CardHeader>
            <CardContent>
              {transaccionesMesQuery.isLoading ? (
                <p className="py-4 text-center text-sm text-muted-foreground">
                  Cargando movimientos...
                </p>
              ) : ultimosMovimientos.length === 0 ? (
                <p className="rounded-lg border border-dashed py-6 text-center text-sm text-muted-foreground">
                  No hay movimientos registrados este mes.
                </p>
              ) : (
                <ul className="divide-y">
                  {ultimosMovimientos.map((tx) => {
                    const esGasto = tx.tipoMovimiento === "GASTO";
                    const esTransferencia =
                      tx.tipoMovimiento === "TRANSFERENCIA";
                    const categoriaNombre =
                      tx.categoriaId !== null
                        ? mapaCategorias.get(tx.categoriaId)
                        : undefined;

                    const titulo =
                      tx.descripcion ||
                      tx.tercero ||
                      categoriaNombre ||
                      (esTransferencia
                        ? `Transferencia a ${tx.cuentaDestinoNombre ?? ""}`
                        : "Movimiento");

                    return (
                      <li
                        key={tx.id}
                        className="flex items-center justify-between gap-3 py-2.5 first:pt-0 last:pb-0"
                      >
                        <div className="flex min-w-0 items-center gap-2.5">
                          <div
                            className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg ${
                              esTransferencia
                                ? "bg-blue-500/10 text-blue-600 dark:text-blue-400"
                                : esGasto
                                  ? "bg-destructive/10 text-destructive"
                                  : "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                            }`}
                          >
                            {esTransferencia ? (
                              <ArrowLeftRight className="h-4 w-4" />
                            ) : esGasto ? (
                              <ArrowUpRight className="h-4 w-4" />
                            ) : (
                              <ArrowDownLeft className="h-4 w-4" />
                            )}
                          </div>
                          <div className="min-w-0">
                            <p className="truncate text-sm font-medium">
                              {titulo}
                            </p>
                            <p className="truncate text-xs text-muted-foreground">
                              {formatearFechaCorta(tx.fecha)} •{" "}
                              {tx.cuentaOrigenNombre}
                            </p>
                          </div>
                        </div>

                        <span
                          className={`shrink-0 text-sm font-semibold tabular-nums ${
                            esTransferencia
                              ? "text-foreground"
                              : esGasto
                                ? "text-destructive"
                                : "text-emerald-600 dark:text-emerald-400"
                          }`}
                        >
                          {esTransferencia ? "" : esGasto ? "-" : "+"}
                          {formatearCentavos(tx.montoCentavos)}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <CardTitle className="text-base">Próximos en agenda</CardTitle>
              <Button variant="ghost" size="sm" className="h-8 gap-1">
                <Link to="/pagos-planificados">
                  Agenda
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </Button>
            </CardHeader>
            <CardContent>
              {pagosQuery.isLoading ? (
                <p className="py-4 text-center text-sm text-muted-foreground">
                  Cargando agenda...
                </p>
              ) : pagosProximos.length === 0 ? (
                <p className="rounded-lg border border-dashed py-6 text-center text-sm text-muted-foreground">
                  No tenés vencimientos ni cobros programados.
                </p>
              ) : (
                <ul className="divide-y">
                  {pagosProximos.map((pago) => {
                    const tx = pago.transaccion;
                    const esIngreso = tx?.tipoMovimiento === "INGRESO";
                    const titulo =
                      tx?.descripcion || tx?.tercero || "Pago programado";

                    return (
                      <li
                        key={pago.id}
                        className="flex items-center justify-between gap-3 py-2.5 first:pt-0 last:pb-0"
                      >
                        <div className="min-w-0 space-y-0.5">
                          <p className="truncate text-sm font-medium">
                            {titulo}
                          </p>
                          <div className="flex items-center gap-2 text-xs text-muted-foreground">
                            <span className="inline-flex items-center gap-1">
                              <CalendarClock className="h-3 w-3" />
                              {formatearFechaCorta(pago.fecha)}
                            </span>
                            <Badge variant="outline" className="text-[10px]">
                              {pago.tipoRecurrencia}
                            </Badge>
                          </div>
                        </div>

                        {tx && (
                          <span
                            className={`shrink-0 text-sm font-semibold tabular-nums ${
                              esIngreso
                                ? "text-emerald-600 dark:text-emerald-400"
                                : "text-destructive"
                            }`}
                          >
                            {esIngreso ? "+" : "-"}
                            {formatearCentavos(tx.montoCentavos)}
                          </span>
                        )}
                      </li>
                    );
                  })}
                </ul>
              )}
            </CardContent>
          </Card>

          {empleados.length > 0 && (
            <Card>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="flex items-center gap-2 text-base">
                  <Users className="h-4 w-4 text-primary" />
                  Nómina activa
                </CardTitle>
                <Button variant="ghost" size="sm" className="h-8 gap-1">
                  <Link to="/empleados">
                    Gestionar
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                </Button>
              </CardHeader>
              <CardContent className="flex items-baseline justify-between">
                <span className="text-xs text-muted-foreground">
                  {empleados.length}{" "}
                  {empleados.length === 1
                    ? "empleado activo"
                    : "empleados activos"}
                </span>
                <span className="text-base font-bold tabular-nums">
                  {formatearCentavos(resumenFinanciero.masaSalarialCentavos)} /
                  mes
                </span>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
