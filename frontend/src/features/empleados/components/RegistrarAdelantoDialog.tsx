import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Banknote } from "lucide-react";
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
import {
  useCategorias,
  useMetodosPago,
} from "@/features/catalogos/hooks/useCatalogos";
import { useCuentas } from "@/features/cuentas/hooks/useCuentas";
import { aplicarErroresCampos, obtenerMensajeError } from "@/lib/errors";
import { pesosACentavos } from "@/lib/money";
import type { EmpleadoResponseDTO } from "@/types/empleados";
import { useRegistrarAdelanto } from "../hooks/useEmpleados";
import {
  crearAdelantoSueldoSchema,
  type CrearAdelantoSueldoFormValues,
} from "../schemas/empleado";

interface RegistrarAdelantoDialogProps {
  empleado: EmpleadoResponseDTO;
}

function obtenerFechaHoyISO(): string {
  const hoy = new Date();
  const anio = hoy.getFullYear();
  const mes = String(hoy.getMonth() + 1).padStart(2, "0");
  const dia = String(hoy.getDate()).padStart(2, "0");
  return `${anio}-${mes}-${dia}`;
}

export function RegistrarAdelantoDialog({
  empleado,
}: RegistrarAdelantoDialogProps) {
  const [open, setOpen] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);

  const cuentasQuery = useCuentas();
  const categoriasQuery = useCategorias();
  const metodosQuery = useMetodosPago();
  const registrarAdelanto = useRegistrarAdelanto();

  const cuentas = Array.isArray(cuentasQuery.data)
    ? cuentasQuery.data.filter((c) => c.tipo !== "EXTERNA")
    : [];
  const categorias = Array.isArray(categoriasQuery.data)
    ? categoriasQuery.data
    : [];
  const metodos = Array.isArray(metodosQuery.data) ? metodosQuery.data : [];

  const categoriaNominaDefault =
    categorias.find((c) => c.nombre.toLowerCase().includes("sueldo"))?.id ?? 0;

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    setError,
    formState: { errors },
  } = useForm<CrearAdelantoSueldoFormValues>({
    resolver: zodResolver(crearAdelantoSueldoSchema),
    defaultValues: {
      fecha: obtenerFechaHoyISO(),
      montoPesos: undefined,
      motivo: "",
      cuentaOrigenId: 0,
      categoriaId: categoriaNominaDefault,
      metodoPagoId: 0,
    },
  });

  const cuentaOrigenId = watch("cuentaOrigenId");
  const categoriaId = watch("categoriaId");
  const metodoPagoId = watch("metodoPagoId");

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (!nextOpen) {
      setErrorGeneral(null);
      reset({
        fecha: obtenerFechaHoyISO(),
        montoPesos: undefined,
        motivo: "",
        cuentaOrigenId: 0,
        categoriaId: categoriaNominaDefault,
        metodoPagoId: 0,
      });
    } else if (categoriaId === 0 && categoriaNominaDefault > 0) {
      setValue("categoriaId", categoriaNominaDefault);
    }
  };

  const onSubmit = (values: CrearAdelantoSueldoFormValues) => {
    setErrorGeneral(null);

    registrarAdelanto.mutate(
      {
        empleadoId: empleado.id,
        dto: {
          fecha: values.fecha,
          montoCentavos: pesosACentavos(values.montoPesos),
          motivo: values.motivo ? values.motivo : undefined,
          cuentaOrigenId: values.cuentaOrigenId,
          categoriaId: values.categoriaId,
          metodoPagoId: values.metodoPagoId,
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

  const cuentaNombre =
    cuentas.find((c) => c.id === cuentaOrigenId)?.nombre ??
    "Seleccioná una cuenta";
  const categoriaNombre =
    categorias.find((c) => c.id === categoriaId)?.nombre ??
    "Seleccioná una categoría";
  const metodoNombre =
    metodos.find((m) => m.id === metodoPagoId)?.nombre ??
    "Seleccioná un método";

  return (
    <>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="h-9 gap-1.5"
        onClick={() => handleOpenChange(true)}
      >
        <Banknote className="h-3.5 w-3.5" />
        Adelanto
      </Button>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              Adelanto de sueldo — {empleado.nombre} {empleado.apellido}
            </DialogTitle>
            <DialogDescription>
              El monto se descontará de la cuenta seleccionada y se deducirá de
              la próxima liquidación.
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
                <Label htmlFor={`adelanto-monto-${empleado.id}`}>
                  Monto ($)
                </Label>
                <Input
                  id={`adelanto-monto-${empleado.id}`}
                  type="number"
                  step="0.01"
                  min="0.01"
                  inputMode="decimal"
                  placeholder="0.00"
                  className="h-11 text-base md:text-sm"
                  {...register("montoPesos", { valueAsNumber: true })}
                />
                {errors.montoPesos && (
                  <p className="text-xs text-destructive">
                    {errors.montoPesos.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor={`adelanto-fecha-${empleado.id}`}>Fecha</Label>
                <Input
                  id={`adelanto-fecha-${empleado.id}`}
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
            </div>

            <div className="space-y-2">
              <Label>Cuenta origen</Label>
              <Select
                value={cuentaOrigenId > 0 ? String(cuentaOrigenId) : null}
                onValueChange={(val: string | null) => {
                  setValue("cuentaOrigenId", val ? Number(val) : 0, {
                    shouldValidate: true,
                  });
                }}
              >
                <SelectTrigger className="h-11 w-full text-base md:text-sm">
                  <SelectValue>{cuentaNombre}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {cuentas.map((cuenta) => (
                    <SelectItem key={cuenta.id} value={String(cuenta.id)}>
                      {cuenta.nombre}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.cuentaOrigenId && (
                <p className="text-xs text-destructive">
                  {errors.cuentaOrigenId.message}
                </p>
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>Categoría</Label>
                <Select
                  value={categoriaId > 0 ? String(categoriaId) : null}
                  onValueChange={(val: string | null) => {
                    setValue("categoriaId", val ? Number(val) : 0, {
                      shouldValidate: true,
                    });
                  }}
                >
                  <SelectTrigger className="h-11 w-full text-base md:text-sm">
                    <SelectValue>{categoriaNombre}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {categorias.map((cat) => (
                      <SelectItem key={cat.id} value={String(cat.id)}>
                        {cat.nombre}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.categoriaId && (
                  <p className="text-xs text-destructive">
                    {errors.categoriaId.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label>Método de pago</Label>
                <Select
                  value={metodoPagoId > 0 ? String(metodoPagoId) : null}
                  onValueChange={(val: string | null) => {
                    setValue("metodoPagoId", val ? Number(val) : 0, {
                      shouldValidate: true,
                    });
                  }}
                >
                  <SelectTrigger className="h-11 w-full text-base md:text-sm">
                    <SelectValue>{metodoNombre}</SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {metodos.map((metodo) => (
                      <SelectItem key={metodo.id} value={String(metodo.id)}>
                        {metodo.nombre}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.metodoPagoId && (
                  <p className="text-xs text-destructive">
                    {errors.metodoPagoId.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor={`adelanto-motivo-${empleado.id}`}>
                Motivo / Observación (opcional)
              </Label>
              <Input
                id={`adelanto-motivo-${empleado.id}`}
                placeholder="Ej: Adelanto quincenal"
                className="h-11 text-base md:text-sm"
                {...register("motivo")}
              />
              {errors.motivo && (
                <p className="text-xs text-destructive">
                  {errors.motivo.message}
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
                disabled={registrarAdelanto.isPending}
              >
                {registrarAdelanto.isPending
                  ? "Registrando..."
                  : "Confirmar adelanto"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
}
