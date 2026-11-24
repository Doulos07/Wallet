import { useState } from "react";
import { Banknote, Calendar, Clock, Eye } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { obtenerMensajeError } from "@/lib/errors";
import type { EmpleadoResponseDTO } from "@/types/empleados";
import { useEmpleadoDetalle } from "../hooks/useEmpleados";

interface DetalleEmpleadoDialogProps {
  empleado: EmpleadoResponseDTO;
}

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

export function DetalleEmpleadoDialog({
  empleado,
}: DetalleEmpleadoDialogProps) {
  const [open, setOpen] = useState(false);
  const [pestana, setPestana] = useState<"EXTRAS" | "ADELANTOS">("EXTRAS");

  const detalleQuery = useEmpleadoDetalle(open ? empleado.id : null);
  const detalle = detalleQuery.data;

  const sueldoNetoEstimadoCentavos = detalle
    ? Math.max(
        0,
        detalle.sueldoBaseCentavos - detalle.totalAdelantosPendientesCentavos,
      )
    : empleado.sueldoBaseCentavos;

  return (
    <>
      <Button
        type="button"
        variant="secondary"
        size="sm"
        className="h-9 gap-1.5"
        onClick={() => setOpen(true)}
      >
        <Eye className="h-3.5 w-3.5" />
        Detalle
      </Button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              {empleado.nombre} {empleado.apellido}
            </DialogTitle>
            <DialogDescription>
              DNI {empleado.dni} • Historial de horas extra, adelantos y
              liquidaciones
            </DialogDescription>
          </DialogHeader>

          {detalleQuery.isError && (
            <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-sm text-destructive">
              {obtenerMensajeError(detalleQuery.error)}
            </div>
          )}

          {detalleQuery.isLoading || !detalle ? (
            <p className="py-8 text-center text-sm text-muted-foreground">
              Cargando detalle del empleado...
            </p>
          ) : (
            <div className="space-y-5">
              {/* Condiciones y fechas de cobro */}
              <div className="grid gap-3 rounded-xl border bg-muted/30 p-3.5 sm:grid-cols-3">
                <div>
                  <p className="text-xs text-muted-foreground">Sueldo base</p>
                  <p className="text-base font-bold tabular-nums">
                    {formatearCentavos(detalle.sueldoBaseCentavos)}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    Día de pago: {detalle.diaPagoSueldo} de cada mes
                  </p>
                </div>

                <div>
                  <p className="text-xs text-muted-foreground">
                    Próximo sueldo neto est.
                  </p>
                  <p className="text-base font-bold tabular-nums text-emerald-600 dark:text-emerald-400">
                    {formatearCentavos(sueldoNetoEstimadoCentavos)}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {detalle.proximoPagoSueldo
                      ? `Fecha: ${formatearFechaCorta(detalle.proximoPagoSueldo)}`
                      : "Sin fecha programada"}
                  </p>
                </div>

                <div>
                  <p className="text-xs text-muted-foreground">
                    Extras pendientes de cobro
                  </p>
                  <p className="text-base font-bold tabular-nums">
                    {formatearCentavos(detalle.totalExtrasPendientesCentavos)}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {detalle.proximoPagoExtras
                      ? `Cobro: ${formatearFechaCorta(detalle.proximoPagoExtras)}`
                      : `Día configurado: ${detalle.diaPagoExtras}`}
                  </p>
                </div>
              </div>

              {detalle.totalAdelantosPendientesCentavos > 0 && (
                <div className="flex items-center justify-between rounded-lg border border-amber-500/30 bg-amber-500/10 px-3.5 py-2.5 text-xs">
                  <span className="font-medium text-amber-800 dark:text-amber-300">
                    Adelantos pendientes a descontar del próximo sueldo:
                  </span>
                  <span className="font-bold tabular-nums text-amber-900 dark:text-amber-200">
                    -
                    {formatearCentavos(
                      detalle.totalAdelantosPendientesCentavos,
                    )}
                  </span>
                </div>
              )}

              {/* Selector de historial */}
              <div className="grid grid-cols-2 gap-1.5 rounded-lg bg-muted p-1">
                <Button
                  type="button"
                  variant={pestana === "EXTRAS" ? "default" : "ghost"}
                  className="h-9 gap-1.5 text-xs sm:text-sm"
                  onClick={() => setPestana("EXTRAS")}
                >
                  <Clock className="h-4 w-4" />
                  Horas y extras ({detalle.registrosExtra.length})
                </Button>
                <Button
                  type="button"
                  variant={pestana === "ADELANTOS" ? "default" : "ghost"}
                  className="h-9 gap-1.5 text-xs sm:text-sm"
                  onClick={() => setPestana("ADELANTOS")}
                >
                  <Banknote className="h-4 w-4" />
                  Adelantos ({detalle.adelantos.length})
                </Button>
              </div>

              {/* Listado de horas extra */}
              {pestana === "EXTRAS" ? (
                detalle.registrosExtra.length === 0 ? (
                  <p className="rounded-lg border border-dashed py-8 text-center text-sm text-muted-foreground">
                    No hay horas ni trabajos extra registrados.
                  </p>
                ) : (
                  <ul className="divide-y rounded-lg border">
                    {detalle.registrosExtra.map((extra) => {
                      const estaLiquidado =
                        extra.estadoLiquidacion === "LIQUIDADO";

                      return (
                        <li
                          key={extra.id}
                          className="flex flex-col gap-2 px-3.5 py-3 sm:flex-row sm:items-center sm:justify-between"
                        >
                          <div className="space-y-1">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="text-sm font-medium">
                                {extra.descripcion ||
                                  (extra.cantidadHorasExtra !== null
                                    ? `${extra.cantidadHorasExtra} hs extra`
                                    : "Trabajo adicional")}
                              </span>
                              <Badge
                                variant={
                                  estaLiquidado ? "secondary" : "outline"
                                }
                              >
                                {estaLiquidado ? "Liquidado" : "Pendiente"}
                              </Badge>
                            </div>
                            <div className="flex items-center gap-2 text-xs text-muted-foreground">
                              <span className="inline-flex items-center gap-1">
                                <Calendar className="h-3 w-3" />
                                {formatearFechaCorta(extra.fecha)}
                              </span>
                              {extra.cantidadHorasExtra !== null && (
                                <>
                                  <span>•</span>
                                  <span>{extra.cantidadHorasExtra} hs</span>
                                </>
                              )}
                            </div>
                          </div>

                          <span className="text-sm font-semibold tabular-nums">
                            {formatearCentavos(extra.montoCalculadoCentavos)}
                          </span>
                        </li>
                      );
                    })}
                  </ul>
                )
              ) : detalle.adelantos.length === 0 ? (
                <p className="rounded-lg border border-dashed py-8 text-center text-sm text-muted-foreground">
                  No hay adelantos de sueldo registrados.
                </p>
              ) : (
                <ul className="divide-y rounded-lg border">
                  {detalle.adelantos.map((adelanto) => (
                    <li
                      key={adelanto.id}
                      className="flex flex-col gap-2 px-3.5 py-3 sm:flex-row sm:items-center sm:justify-between"
                    >
                      <div className="space-y-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-sm font-medium">
                            {adelanto.motivo || "Adelanto de sueldo"}
                          </span>
                          <Badge
                            variant={
                              adelanto.descontado ? "secondary" : "outline"
                            }
                          >
                            {adelanto.descontado
                              ? "Descontado"
                              : "Pendiente de descuento"}
                          </Badge>
                        </div>
                        <p className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                          <Calendar className="h-3 w-3" />
                          {formatearFechaCorta(adelanto.fecha)}
                        </p>
                      </div>

                      <span className="text-sm font-semibold tabular-nums text-destructive">
                        -{formatearCentavos(adelanto.montoCentavos)}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
