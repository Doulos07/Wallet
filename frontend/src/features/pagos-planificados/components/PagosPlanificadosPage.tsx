import { useMemo, useState } from "react";
import {
  ArrowDownLeft,
  ArrowUpRight,
  CalendarClock,
  PowerOff,
  Repeat,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  useCategorias,
  useMetodosPago,
} from "@/features/catalogos/hooks/useCatalogos";
import { obtenerMensajeError } from "@/lib/errors";
import type { FrecuenciaPago } from "@/types/enums";
import type { PagoPlanificadoResponseDTO } from "@/types/pagosPlanificados";
import {
  useDesactivarPagoPlanificado,
  usePagosPlanificados,
} from "../hooks/usePagosPlanificados";
import { NuevoPagoPlanificadoDialog } from "./NuevoPagoPlanificadoDialog";

const formateadorARS = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  minimumFractionDigits: 2,
});

const ETIQUETAS_FRECUENCIA_CORTA: Record<FrecuenciaPago, string> = {
  DAYS: "Diario",
  WEEKS: "Semanal",
  MONTHS: "Mensual",
  YEARS: "Anual",
};

function formatearCentavos(centavos: number): string {
  return formateadorARS.format(centavos / 100);
}

function formatearFechaCorta(fechaISO: string): string {
  const soloFecha = fechaISO.slice(0, 10);
  const partes = soloFecha.split("-");
  if (partes.length !== 3) return fechaISO;
  return `${partes[2]}/${partes[1]}/${partes[0]}`;
}

function describirRecurrencia(pago: PagoPlanificadoResponseDTO): string {
  const freq = pago.frecuencia
    ? ETIQUETAS_FRECUENCIA_CORTA[pago.frecuencia]
    : "";

  switch (pago.tipoRecurrencia) {
    case "UNICO":
      return "Pago único";
    case "INFINITO":
      return `${freq} • Indefinido`;
    case "HASTA_FECHA":
      return `${freq} • Hasta ${pago.fechaFin ? formatearFechaCorta(pago.fechaFin) : ""}`;
    case "POR_EVENTO":
      return `${freq} • ${pago.eventoActual ?? 0}/${pago.cantidadEventos ?? 0} ejecutados`;
  }
}

export function PagosPlanificadosPage() {
  const [errorAccion, setErrorAccion] = useState<string | null>(null);

  const pagosQuery = usePagosPlanificados();
  const desactivarPago = useDesactivarPagoPlanificado();
  const categoriasQuery = useCategorias();
  const metodosQuery = useMetodosPago();

  const mapaCategorias = useMemo(() => {
    const lista = Array.isArray(categoriasQuery.data)
      ? categoriasQuery.data
      : [];
    return new Map<number, string>(lista.map((c) => [c.id, c.nombre]));
  }, [categoriasQuery.data]);

  const mapaMetodos = useMemo(() => {
    const lista = Array.isArray(metodosQuery.data) ? metodosQuery.data : [];
    return new Map<number, string>(lista.map((m) => [m.id, m.nombre]));
  }, [metodosQuery.data]);

  const pagos = Array.isArray(pagosQuery.data) ? pagosQuery.data : [];

  const handleDesactivar = (pago: PagoPlanificadoResponseDTO) => {
    const etiqueta =
      pago.transaccion?.descripcion ||
      pago.transaccion?.tercero ||
      "este pago planificado";
    const confirmado = window.confirm(
      `¿Desactivar "${etiqueta}"? No se volverán a generar movimientos automáticos.`,
    );
    if (!confirmado) return;

    setErrorAccion(null);
    desactivarPago.mutate(pago.id, {
      onError: (err: unknown) => {
        setErrorAccion(obtenerMensajeError(err));
      },
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Agenda</h1>
          <p className="text-sm text-muted-foreground">
            Pagos e ingresos programados y recurrentes
          </p>
        </div>
        <NuevoPagoPlanificadoDialog />
      </div>

      {errorAccion && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {errorAccion}
        </div>
      )}

      {pagosQuery.isError && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {obtenerMensajeError(pagosQuery.error)}
        </div>
      )}

      {pagosQuery.isLoading ? (
        <p className="py-8 text-center text-sm text-muted-foreground">
          Cargando agenda...
        </p>
      ) : pagos.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center gap-3 py-12 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary">
              <CalendarClock className="h-6 w-6" />
            </div>
            <div className="space-y-1">
              <p className="font-medium">No tenés pagos planificados activos</p>
              <p className="text-sm text-muted-foreground">
                Programá vencimientos, suscripciones o cobros recurrentes con el
                botón de arriba.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-2.5">
          {pagos.map((pago) => {
            const tx = pago.transaccion;
            const esIngreso = tx?.tipoMovimiento === "INGRESO";

            const nombreCategoria =
              tx && tx.categoriaId !== null
                ? mapaCategorias.get(tx.categoriaId)
                : undefined;
            const nombreMetodo =
              tx && tx.metodoPagoId !== null
                ? mapaMetodos.get(tx.metodoPagoId)
                : undefined;

            const nombreCuenta = !tx
              ? ""
              : tx.cuentaOrigenNombre === "__CUENTA_EXTERNA__"
                ? (tx.cuentaDestinoNombre ?? "")
                : tx.cuentaOrigenNombre;

            const tituloPrincipal =
              tx?.descripcion ||
              tx?.tercero ||
              nombreCategoria ||
              "Pago programado";

            return (
              <Card key={pago.id}>
                <CardContent className="flex flex-col gap-3 py-3.5 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-start gap-3">
                    <div
                      className={`mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${
                        esIngreso
                          ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                          : "bg-destructive/10 text-destructive"
                      }`}
                    >
                      {esIngreso ? (
                        <ArrowDownLeft className="h-5 w-5" />
                      ) : (
                        <ArrowUpRight className="h-5 w-5" />
                      )}
                    </div>

                    <div className="space-y-1.5">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-medium">{tituloPrincipal}</span>
                        <Badge variant="secondary" className="gap-1">
                          <Repeat className="h-3 w-3" />
                          {describirRecurrencia(pago)}
                        </Badge>
                      </div>

                      <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted-foreground">
                        <span className="font-medium text-foreground">
                          Próxima fecha: {formatearFechaCorta(pago.fecha)}
                        </span>
                        {nombreCuenta && (
                          <>
                            <span>•</span>
                            <span>{nombreCuenta}</span>
                          </>
                        )}
                        {nombreCategoria && (
                          <>
                            <span>•</span>
                            <span>{nombreCategoria}</span>
                          </>
                        )}
                        {nombreMetodo && (
                          <>
                            <span>•</span>
                            <span>{nombreMetodo}</span>
                          </>
                        )}
                        {tx?.tercero && tx?.descripcion && (
                          <>
                            <span>•</span>
                            <span>{tx.tercero}</span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between gap-3 sm:justify-end">
                    {tx && (
                      <span
                        className={`text-base font-semibold tabular-nums ${
                          esIngreso
                            ? "text-emerald-600 dark:text-emerald-400"
                            : "text-destructive"
                        }`}
                      >
                        {esIngreso ? "+" : "-"}
                        {formatearCentavos(tx.montoCentavos)}
                      </span>
                    )}

                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="h-9 w-9 text-muted-foreground hover:text-destructive"
                      disabled={desactivarPago.isPending}
                      onClick={() => handleDesactivar(pago)}
                      aria-label="Desactivar pago planificado"
                      title="Desactivar pago planificado"
                    >
                      <PowerOff className="h-4 w-4" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
