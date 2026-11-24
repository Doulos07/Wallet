import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { aplicarErroresCampos, obtenerMensajeError } from "@/lib/errors";
import { parsearNumeroOpcional } from "@/lib/forms";
import { pesosACentavos } from "@/lib/money";
import type { EmpleadoResponseDTO } from "@/types/empleados";
import { useRegistrarExtra } from "../hooks/useEmpleados";
import {
  crearRegistroExtraSchema,
  type CrearRegistroExtraFormValues,
} from "../schemas/empleado";

interface RegistrarExtraDialogProps {
  empleado: EmpleadoResponseDTO;
}

function obtenerFechaHoyISO(): string {
  const hoy = new Date();
  const anio = hoy.getFullYear();
  const mes = String(hoy.getMonth() + 1).padStart(2, "0");
  const dia = String(hoy.getDate()).padStart(2, "0");
  return `${anio}-${mes}-${dia}`;
}

export function RegistrarExtraDialog({ empleado }: RegistrarExtraDialogProps) {
  const [open, setOpen] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);

  const registrarExtra = useRegistrarExtra();
  const esPorTrabajo = empleado.tipoHoraExtra === "POR_TRABAJO_REALIZADO";

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<CrearRegistroExtraFormValues>({
    resolver: zodResolver(crearRegistroExtraSchema),
    defaultValues: {
      fecha: obtenerFechaHoyISO(),
      cantidadHorasExtra: undefined,
      montoManualPesos: undefined,
      descripcion: "",
    },
  });

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (!nextOpen) {
      setErrorGeneral(null);
      reset({
        fecha: obtenerFechaHoyISO(),
        cantidadHorasExtra: undefined,
        montoManualPesos: undefined,
        descripcion: "",
      });
    }
  };

  const onSubmit = (values: CrearRegistroExtraFormValues) => {
    setErrorGeneral(null);

    const horas = values.cantidadHorasExtra;
    const montoManual = values.montoManualPesos;

    const tieneHoras =
      typeof horas === "number" && Number.isFinite(horas) && horas > 0;
    const tieneMontoManual =
      typeof montoManual === "number" &&
      Number.isFinite(montoManual) &&
      montoManual > 0;

    registrarExtra.mutate(
      {
        empleadoId: empleado.id,
        dto: {
          fecha: values.fecha,
          cantidadHorasExtra: tieneHoras ? horas : null,
          montoManualCentavos: tieneMontoManual
            ? pesosACentavos(montoManual)
            : null,
          descripcion: values.descripcion ? values.descripcion : undefined,
        },
      },
      {
        onSuccess: () => {
          handleOpenChange(false);
        },
        onError: (err: unknown) => {
          const hayErroresCampo = aplicarErroresCampos(err, setError);
          if (!hayErroresCampo) {
            setErrorGeneral(obtenerMensajeError(err));
          }
        },
      },
    );
  };

  return (
    <>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="h-9 gap-1.5"
        onClick={() => setOpen(true)}
      >
        <Clock className="h-3.5 w-3.5" />
        Horas / Extra
      </Button>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              Registrar extra — {empleado.nombre} {empleado.apellido}
            </DialogTitle>
            <DialogDescription>
              {esPorTrabajo
                ? "Cargá el importe del trabajo adicional realizado."
                : "Ingresá las horas extra trabajadas o un importe fijo manual."}
            </DialogDescription>
          </DialogHeader>

          <form
            onSubmit={handleSubmit(onSubmit)}
            className="space-y-4"
            noValidate
          >
            {errorGeneral && (
              <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-sm text-destructive">
                {errorGeneral}
              </div>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor={`extra-fecha-${empleado.id}`}>Fecha</Label>{" "}
                <Input
                  id={`extra-fecha-${empleado.id}`}
                  type="date"
                  className="h-11 text-base md:text-sm"
                  {...register("fecha")}
                />
                {errors.fecha && (
                  <p className="text-xs text-destructive">
                    {errors.fecha.message}
                  </p>
                )}
              </div>

              {!esPorTrabajo && (
                <div className="space-y-2">
                  <Label htmlFor={`extra-horas-${empleado.id}`}>
                    {" "}
                    Cantidad de horas{" "}
                  </Label>{" "}
                  <Input
                    id={`extra-horas-${empleado.id}`}
                    type="number"
                    step="0.5"
                    min="0.5"
                    inputMode="decimal"
                    placeholder="Ej: 2.5"
                    className="h-11 text-base md:text-sm"
                    {...register("cantidadHorasExtra", {
                      setValueAs: parsearNumeroOpcional,
                    })}
                  />
                  {errors.cantidadHorasExtra && (
                    <p className="text-xs text-destructive">
                      {errors.cantidadHorasExtra.message}
                    </p>
                  )}
                </div>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor={`extra-monto-${empleado.id}`}>
                {" "}
                {esPorTrabajo
                  ? "Monto del trabajo ($)"
                  : "Monto manual ($) (opcional si cargaste horas)"}{" "}
              </Label>{" "}
              <Input
                id={`extra-monto-${empleado.id}`}
                type="number"
                step="0.01"
                min="0.01"
                inputMode="decimal"
                placeholder="0.00"
                className="h-11 text-base md:text-sm"
                {...register("montoManualPesos", {
                  setValueAs: parsearNumeroOpcional,
                })}
              />
              {errors.montoManualPesos && (
                <p className="text-xs text-destructive">
                  {errors.montoManualPesos.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor={`extra-desc-${empleado.id}`}>
                {" "}
                Detalle / Tarea realizada (opcional){" "}
              </Label>{" "}
              <Input
                id={`extra-desc-${empleado.id}`}
                placeholder="Ej: Guardia sábado, instalación..."
                className="h-11 text-base md:text-sm"
                {...register("descripcion")}
              />
              {errors.descripcion && (
                <p className="text-xs text-destructive">
                  {errors.descripcion.message}
                </p>
              )}
            </div>

            <DialogFooter className="gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                className="h-11"
                onClick={() => handleOpenChange(false)}
              >
                Cancelar
              </Button>
              <Button
                type="submit"
                className="h-11"
                disabled={registrarExtra.isPending}
              >
                {registrarExtra.isPending ? "Guardando..." : "Registrar extra"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
}
