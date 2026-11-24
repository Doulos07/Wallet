import { useState } from "react";
import { Calendar, UserMinus, Users } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { obtenerMensajeError } from "@/lib/errors";
import type { EmpleadoResponseDTO } from "@/types/empleados";
import { useDesactivarEmpleado, useEmpleados } from "../hooks/useEmpleados";
import { EditarCondicionesDialog } from "./EditarCondicionesDialog";
import { NuevoEmpleadoDialog } from "./NuevoEmpleadoDialog";
import { RegistrarAdelantoDialog } from "./RegistrarAdelantoDialog";
import { RegistrarExtraDialog } from "./RegistrarExtraDialog";
import { DetalleEmpleadoDialog } from "./DetalleEmpleadoDialog";

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

function describirEsquemaExtra(emp: EmpleadoResponseDTO): string {
  switch (emp.tipoHoraExtra) {
    case "MULTIPLICADOR_HORA":
      return `Hora extra ${emp.valorHoraExtra}x`;
    case "MONTO_FIJO_HORA":
      return `Hora extra ${formateadorARS.format(emp.valorHoraExtra)}/h`;
    case "POR_TRABAJO_REALIZADO":
      return "Extras por trabajo realizado";
  }
}

export function EmpleadosPage() {
  const [errorAccion, setErrorAccion] = useState<string | null>(null);

  const empleadosQuery = useEmpleados();
  const desactivarEmpleado = useDesactivarEmpleado();

  const empleados = Array.isArray(empleadosQuery.data)
    ? empleadosQuery.data
    : [];

  const masaSalarialCentavos = empleados.reduce(
    (acc, emp) => acc + emp.sueldoBaseCentavos,
    0,
  );

  const handleDesactivar = (emp: EmpleadoResponseDTO) => {
    const confirmado = window.confirm(
      `¿Dar de baja a ${emp.nombre} ${emp.apellido}? Se detendrán sus liquidaciones automáticas.`,
    );
    if (!confirmado) return;

    setErrorAccion(null);
    desactivarEmpleado.mutate(emp.id, {
      onError: (err: unknown) => {
        setErrorAccion(obtenerMensajeError(err));
      },
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Nómina</h1>
          <p className="text-sm text-muted-foreground">
            Gestión de empleados, horas extra, adelantos y sueldos programados
          </p>
        </div>
        <NuevoEmpleadoDialog />
      </div>

      {empleados.length > 0 && (
        <Card>
          <CardContent className="flex flex-col gap-1 py-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                Masa salarial base mensual ({empleados.length}{" "}
                {empleados.length === 1
                  ? "empleado activo"
                  : "empleados activos"}
                )
              </p>
              <p className="text-2xl font-bold tabular-nums">
                {formatearCentavos(masaSalarialCentavos)}
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {errorAccion && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {errorAccion}
        </div>
      )}

      {empleadosQuery.isError && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {obtenerMensajeError(empleadosQuery.error)}
        </div>
      )}

      {empleadosQuery.isLoading ? (
        <p className="py-8 text-center text-sm text-muted-foreground">
          Cargando nómina...
        </p>
      ) : empleados.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center gap-3 py-12 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10 text-primary">
              <Users className="h-6 w-6" />
            </div>
            <div className="space-y-1">
              <p className="font-medium">No tenés empleados activos</p>
              <p className="text-sm text-muted-foreground">
                Agregá un empleado para automatizar sus pagos mensuales y
                registrar extras o adelantos.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {empleados.map((emp) => {
            const valorHoraBasePesos =
              emp.horasMensualesBase > 0
                ? emp.sueldoBaseCentavos / 100 / emp.horasMensualesBase
                : 0;

            return (
              <Card key={emp.id}>
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <CardTitle className="text-lg">
                        {emp.nombre} {emp.apellido}
                      </CardTitle>
                      <p className="text-xs text-muted-foreground">
                        DNI {emp.dni} • {emp.horasMensualesBase} hs/mes (
                        {formateadorARS.format(valorHoraBasePesos)}/h)
                      </p>
                    </div>

                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="h-9 w-9 shrink-0 text-muted-foreground hover:text-destructive"
                      disabled={desactivarEmpleado.isPending}
                      onClick={() => handleDesactivar(emp)}
                      aria-label={`Desactivar a ${emp.nombre} ${emp.apellido}`}
                      title="Dar de baja"
                    >
                      <UserMinus className="h-4 w-4" />
                    </Button>
                  </div>
                </CardHeader>

                <CardContent className="space-y-4">
                  <div className="flex flex-wrap items-baseline justify-between gap-2">
                    <div>
                      <p className="text-xs text-muted-foreground">
                        Sueldo base
                      </p>
                      <p className="text-xl font-bold tabular-nums">
                        {formatearCentavos(emp.sueldoBaseCentavos)}
                      </p>
                    </div>

                    <div className="flex flex-wrap gap-1.5">
                      <Badge variant="secondary">
                        {describirEsquemaExtra(emp)}
                      </Badge>
                      {emp.proximoPagoSueldo && (
                        <Badge variant="outline" className="gap-1">
                          <Calendar className="h-3 w-3" />
                          Próximo pago:{" "}
                          {formatearFechaCorta(emp.proximoPagoSueldo)}
                        </Badge>
                      )}
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-2 border-t pt-3">
                    <DetalleEmpleadoDialog empleado={emp} />
                    <RegistrarExtraDialog empleado={emp} />
                    <RegistrarAdelantoDialog empleado={emp} />
                    <EditarCondicionesDialog empleado={emp} />
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
