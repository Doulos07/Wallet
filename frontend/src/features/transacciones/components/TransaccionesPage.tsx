import { useMemo, useState } from "react";
import {
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowUpRight,
  Ban,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  useCategorias,
  useMetodosPago,
} from "@/features/catalogos/hooks/useCatalogos";
import { useCuentas } from "@/features/cuentas/hooks/useCuentas";
import { obtenerMensajeError } from "@/lib/errors";
import type { TransaccionResponseDTO } from "@/types/transacciones";
import {
  useAnularTransaccion,
  useAutorizarTransaccion,
  useTransacciones,
} from "../hooks/useTransacciones";
import { NuevaTransaccionDialog } from "./NuevaTransaccionDialog";

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

export function TransaccionesPage() {
  const [page, setPage] = useState(0);
  const [cuentaIdFiltro, setCuentaIdFiltro] = useState<number | undefined>(
    undefined,
  );
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [errorAccion, setErrorAccion] = useState<string | null>(null);

  const cuentasQuery = useCuentas();
  const categoriasQuery = useCategorias();
  const metodosQuery = useMetodosPago();

  const transaccionesQuery = useTransacciones({
    page,
    size: 20,
    cuentaId: cuentaIdFiltro,
    desde: desde || undefined,
    hasta: hasta || undefined,
  });

  const anularTransaccion = useAnularTransaccion();
  const autorizarTransaccion = useAutorizarTransaccion();

  const cuentas = Array.isArray(cuentasQuery.data)
    ? cuentasQuery.data.filter((c) => c.tipo !== "EXTERNA")
    : [];

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

  const pagina = transaccionesQuery.data;
  const movimientos = pagina?.content ?? [];

  const limpiarFiltros = () => {
    setCuentaIdFiltro(undefined);
    setDesde("");
    setHasta("");
    setPage(0);
  };

  const handleAutorizar = (tx: TransaccionResponseDTO) => {
    const confirmado = window.confirm(
      `¿Autorizar este movimiento de ${formatearCentavos(tx.montoCentavos)}? Se registrará el asiento contable y se actualizará el saldo de la cuenta.`,
    );
    if (!confirmado) return;

    setErrorAccion(null);
    autorizarTransaccion.mutate(tx.id, {
      onError: (err: unknown) => {
        setErrorAccion(obtenerMensajeError(err));
      },
    });
  };

  const handleAnular = (tx: TransaccionResponseDTO) => {
    const confirmado = window.confirm(
      `¿Anular este movimiento de ${formatearCentavos(tx.montoCentavos)}? Se generará un contra-asiento automático.`,
    );
    if (!confirmado) return;

    setErrorAccion(null);
    anularTransaccion.mutate(tx.id, {
      onError: (err: unknown) => {
        setErrorAccion(obtenerMensajeError(err));
      },
    });
  };

  const cuentaFiltroNombre =
    cuentaIdFiltro !== undefined
      ? (cuentas.find((c) => c.id === cuentaIdFiltro)?.nombre ??
        "Todas las cuentas")
      : "Todas las cuentas";

  const hayFiltrosActivos =
    cuentaIdFiltro !== undefined || desde !== "" || hasta !== "";

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Movimientos</h1>
          <p className="text-sm text-muted-foreground">
            Historial de ingresos, gastos y transferencias entre cuentas
          </p>
        </div>
        <NuevaTransaccionDialog />
      </div>

      <Card>
        <CardContent className="grid gap-3 pt-4 sm:grid-cols-4 sm:items-end">
          <div className="space-y-1.5">
            <Label>Cuenta</Label>
            <Select
              value={
                cuentaIdFiltro !== undefined ? String(cuentaIdFiltro) : "ALL"
              }
              onValueChange={(val: string | null) => {
                setCuentaIdFiltro(
                  val && val !== "ALL" ? Number(val) : undefined,
                );
                setPage(0);
              }}
            >
              <SelectTrigger className="h-10 w-full">
                <SelectValue>{cuentaFiltroNombre}</SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Todas las cuentas</SelectItem>
                {cuentas.map((cuenta) => (
                  <SelectItem key={cuenta.id} value={String(cuenta.id)}>
                    {cuenta.nombre}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="filtro-desde">Desde</Label>
            <Input
              id="filtro-desde"
              type="date"
              className="h-10"
              value={desde}
              onChange={(e) => {
                setDesde(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="filtro-hasta">Hasta</Label>
            <Input
              id="filtro-hasta"
              type="date"
              className="h-10"
              value={hasta}
              onChange={(e) => {
                setHasta(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <Button
            type="button"
            variant="outline"
            className="h-10 gap-1.5"
            disabled={!hayFiltrosActivos}
            onClick={limpiarFiltros}
          >
            <RotateCcw className="h-4 w-4" />
            Limpiar filtros
          </Button>
        </CardContent>
      </Card>

      {errorAccion && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {errorAccion}
        </div>
      )}

      {transaccionesQuery.isError && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {obtenerMensajeError(transaccionesQuery.error)}
        </div>
      )}

      {transaccionesQuery.isLoading ? (
        <p className="py-8 text-center text-sm text-muted-foreground">
          Cargando movimientos...
        </p>
      ) : movimientos.length === 0 ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-muted-foreground">
            No se encontraron movimientos para los filtros seleccionados.
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-2.5">
          {movimientos.map((tx) => {
            const esGasto = tx.tipoMovimiento === "GASTO";
            const esTransferencia = tx.tipoMovimiento === "TRANSFERENCIA";
            const esAnulada = String(tx.estadoNegocio).startsWith("ANULAD");
            const pendienteAutorizar =
              !esAnulada && tx.estadoBancario === "NO_LIQUIDADO";

            const nombreCategoria =
              tx.categoriaId !== null
                ? mapaCategorias.get(tx.categoriaId)
                : undefined;
            const nombreMetodo =
              tx.metodoPagoId !== null
                ? mapaMetodos.get(tx.metodoPagoId)
                : undefined;

            const tituloPrincipal =
              tx.descripcion ||
              tx.tercero ||
              nombreCategoria ||
              (esTransferencia
                ? `Transferencia a ${tx.cuentaDestinoNombre ?? "cuenta"}`
                : "Movimiento");

            return (
              <Card
                key={tx.id}
                className={
                  esAnulada
                    ? "opacity-60"
                    : pendienteAutorizar
                      ? "border-amber-500/40"
                      : undefined
                }
              >
                <CardContent className="flex flex-col gap-3 py-3.5 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-start gap-3">
                    <div
                      className={`mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${
                        esTransferencia
                          ? "bg-blue-500/10 text-blue-600 dark:text-blue-400"
                          : esGasto
                            ? "bg-destructive/10 text-destructive"
                            : "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                      }`}
                    >
                      {esTransferencia ? (
                        <ArrowLeftRight className="h-5 w-5" />
                      ) : esGasto ? (
                        <ArrowUpRight className="h-5 w-5" />
                      ) : (
                        <ArrowDownLeft className="h-5 w-5" />
                      )}
                    </div>

                    <div className="space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className={`font-medium ${
                            esAnulada ? "line-through" : ""
                          }`}
                        >
                          {tituloPrincipal}
                        </span>
                        {esAnulada && (
                          <Badge variant="destructive">Anulado</Badge>
                        )}
                        {pendienteAutorizar && (
                          <Badge className="bg-amber-500/15 text-amber-700 hover:bg-amber-500/20 dark:text-amber-300">
                            Pendiente de autorizar
                          </Badge>
                        )}
                        {tx.garantiaMeses !== null && tx.garantiaMeses > 0 && (
                          <Badge variant="outline">
                            Garantía {tx.garantiaMeses}m
                          </Badge>
                        )}
                      </div>

                      <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted-foreground">
                        <span>{formatearFechaCorta(tx.fecha)}</span>
                        <span>•</span>
                        <span>
                          {esTransferencia
                            ? `${tx.cuentaOrigenNombre} → ${tx.cuentaDestinoNombre ?? ""}`
                            : tx.cuentaOrigenNombre}
                        </span>
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
                        {tx.tercero && tx.descripcion && (
                          <>
                            <span>•</span>
                            <span>{tx.tercero}</span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between gap-2 sm:justify-end">
                    <span
                      className={`mr-1 text-base font-semibold tabular-nums ${
                        esAnulada
                          ? "text-muted-foreground line-through"
                          : esTransferencia
                            ? "text-foreground"
                            : esGasto
                              ? "text-destructive"
                              : "text-emerald-600 dark:text-emerald-400"
                      }`}
                    >
                      {esTransferencia ? "" : esGasto ? "-" : "+"}
                      {formatearCentavos(tx.montoCentavos)}
                    </span>

                    {pendienteAutorizar && (
                      <Button
                        type="button"
                        size="sm"
                        className="h-9 gap-1.5"
                        disabled={
                          autorizarTransaccion.isPending ||
                          anularTransaccion.isPending
                        }
                        onClick={() => handleAutorizar(tx)}
                      >
                        <CheckCircle2 className="h-4 w-4" />
                        Autorizar
                      </Button>
                    )}

                    {!esAnulada && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="h-9 w-9 text-muted-foreground hover:text-destructive"
                        disabled={
                          anularTransaccion.isPending ||
                          autorizarTransaccion.isPending
                        }
                        onClick={() => handleAnular(tx)}
                        aria-label="Anular movimiento"
                        title="Anular movimiento"
                      >
                        <Ban className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {pagina && pagina.totalPages > 1 && (
        <div className="flex items-center justify-between pt-2">
          <p className="text-xs text-muted-foreground">
            Página {pagina.number + 1} de {pagina.totalPages} (
            {pagina.totalElements} movimientos)
          </p>
          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={pagina.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              <ChevronLeft className="h-4 w-4" />
              Anterior
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={pagina.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Siguiente
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
