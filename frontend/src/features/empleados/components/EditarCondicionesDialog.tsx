import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { SlidersHorizontal } from "lucide-react";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { aplicarErroresCampos, obtenerMensajeError } from "@/lib/errors";
import { pesosACentavos } from "@/lib/money";
import type { EmpleadoResponseDTO } from "@/types/empleados";
import type { TipoPagoHoraExtra } from "@/types/enums";
import { useActualizarCondicionesEmpleado } from "../hooks/useEmpleados";
import {
  actualizarEmpleadoSchema,
  TIPOS_PAGO_HORA_EXTRA,
  type ActualizarEmpleadoFormValues,
} from "../schemas/empleado";

interface EditarCondicionesDialogProps {
  empleado: EmpleadoResponseDTO;
}

const ETIQUETAS_TIPO_EXTRA: Record<TipoPagoHoraExtra, string> = {
  MULTIPLICADOR_HORA: "Multiplicador sobre hora base",
  MONTO_FIJO_HORA: "Monto fijo por hora ($)",
  POR_TRABAJO_REALIZADO: "Por trabajo / monto manual",
};

export function EditarCondicionesDialog({
  empleado,
}: EditarCondicionesDialogProps) {
  const [open, setOpen] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);

  const actualizarCondiciones = useActualizarCondicionesEmpleado();

  const valoresIniciales = (): ActualizarEmpleadoFormValues => ({
    sueldoBasePesos: Number((empleado.sueldoBaseCentavos / 100).toFixed(2)),
    horasMensualesBase: empleado.horasMensualesBase,
    tipoHoraExtra: empleado.tipoHoraExtra,
    valorHoraExtra: Number(empleado.valorHoraExtra),
    diaPagoSueldo: empleado.diaPagoSueldo ?? 10,
    diaPagoExtras: empleado.diaPagoExtras ?? 20,
  });

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    setError,
    formState: { errors },
  } = useForm<ActualizarEmpleadoFormValues>({
    resolver: zodResolver(actualizarEmpleadoSchema),
    defaultValues: valoresIniciales(),
  });

  const tipoHoraExtra = watch("tipoHoraExtra");

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    setErrorGeneral(null);
    reset(valoresIniciales());
  };

  const onSubmit = (values: ActualizarEmpleadoFormValues) => {
    setErrorGeneral(null);

    actualizarCondiciones.mutate(
      {
        empleadoId: empleado.id,
        dto: {
          sueldoBaseCentavos: pesosACentavos(values.sueldoBasePesos),
          horasMensualesBase: values.horasMensualesBase,
          tipoHoraExtra: values.tipoHoraExtra,
          valorHoraExtra:
            values.tipoHoraExtra === "POR_TRABAJO_REALIZADO"
              ? 0
              : values.valorHoraExtra,
          diaPagoSueldo: values.diaPagoSueldo,
          diaPagoExtras: values.diaPagoExtras,
        },
      },
      {
        onSuccess: () => {
          setOpen(false);
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
        onClick={() => handleOpenChange(true)}
      >
        <SlidersHorizontal className="h-3.5 w-3.5" />
        Condiciones
      </Button>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              Condiciones — {empleado.nombre} {empleado.apellido}
            </DialogTitle>
            <DialogDescription>
              Actualizá el sueldo base, las horas mensuales o los días de pago.
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
                <Label htmlFor={`edit-sueldo-${empleado.id}`}>
                  Sueldo base ($)
                </Label>
                <Input
                  id={`edit-sueldo-${empleado.id}`}
                  type="number"
                  step="0.01"
                  min="0.01"
                  inputMode="decimal"
                  className="h-11 text-base md:text-sm"
                  {...register("sueldoBasePesos", { valueAsNumber: true })}
                />
                {errors.sueldoBasePesos && (
                  <p className="text-xs text-destructive">
                    {errors.sueldoBasePesos.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor={`edit-horas-${empleado.id}`}>
                  Horas mensuales
                </Label>
                <Input
                  id={`edit-horas-${empleado.id}`}
                  type="number"
                  min="1"
                  max="744"
                  inputMode="numeric"
                  className="h-11 text-base md:text-sm"
                  {...register("horasMensualesBase", { valueAsNumber: true })}
                />
                {errors.horasMensualesBase && (
                  <p className="text-xs text-destructive">
                    {errors.horasMensualesBase.message}
                  </p>
                )}
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>Esquema de horas extra</Label>
                <Select
                  value={tipoHoraExtra}
                  onValueChange={(val: string | null) => {
                    if (val) {
                      setValue("tipoHoraExtra", val as TipoPagoHoraExtra, {
                        shouldValidate: true,
                      });
                    }
                  }}
                >
                  <SelectTrigger className="h-11 w-full text-base md:text-sm">
                    <SelectValue>
                      {ETIQUETAS_TIPO_EXTRA[tipoHoraExtra]}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {TIPOS_PAGO_HORA_EXTRA.map((tipo) => (
                      <SelectItem key={tipo} value={tipo}>
                        {ETIQUETAS_TIPO_EXTRA[tipo]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {tipoHoraExtra !== "POR_TRABAJO_REALIZADO" && (
                <div className="space-y-2">
                  <Label htmlFor={`edit-val-extra-${empleado.id}`}>
                    {tipoHoraExtra === "MULTIPLICADOR_HORA"
                      ? "Multiplicador"
                      : "Valor fijo / hora ($)"}
                  </Label>
                  <Input
                    id={`edit-val-extra-${empleado.id}`}
                    type="number"
                    step="0.01"
                    min="0"
                    inputMode="decimal"
                    className="h-11 text-base md:text-sm"
                    {...register("valorHoraExtra", { valueAsNumber: true })}
                  />
                  {errors.valorHoraExtra && (
                    <p className="text-xs text-destructive">
                      {errors.valorHoraExtra.message}
                    </p>
                  )}
                </div>
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor={`edit-dia-sueldo-${empleado.id}`}>
                  Día pago sueldo
                </Label>
                <Input
                  id={`edit-dia-sueldo-${empleado.id}`}
                  type="number"
                  min="1"
                  max="31"
                  inputMode="numeric"
                  className="h-11 text-base md:text-sm"
                  {...register("diaPagoSueldo", { valueAsNumber: true })}
                />
                {errors.diaPagoSueldo && (
                  <p className="text-xs text-destructive">
                    {errors.diaPagoSueldo.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor={`edit-dia-extras-${empleado.id}`}>
                  Día pago extras
                </Label>
                <Input
                  id={`edit-dia-extras-${empleado.id}`}
                  type="number"
                  min="1"
                  max="31"
                  inputMode="numeric"
                  className="h-11 text-base md:text-sm"
                  {...register("diaPagoExtras", { valueAsNumber: true })}
                />
                {errors.diaPagoExtras && (
                  <p className="text-xs text-destructive">
                    {errors.diaPagoExtras.message}
                  </p>
                )}
              </div>
            </div>

            <DialogFooter className="gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                className="h-11"
                onClick={() => setOpen(false)}
              >
                Cancelar
              </Button>
              <Button
                type="submit"
                className="h-11"
                disabled={actualizarCondiciones.isPending}
              >
                {actualizarCondiciones.isPending
                  ? "Actualizando..."
                  : "Guardar cambios"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
}
