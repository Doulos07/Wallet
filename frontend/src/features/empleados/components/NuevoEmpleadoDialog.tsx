import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { UserPlus } from "lucide-react";
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
import type { TipoPagoHoraExtra } from "@/types/enums";
import { useCrearEmpleado } from "../hooks/useEmpleados";
import {
  crearEmpleadoSchema,
  TIPOS_PAGO_HORA_EXTRA,
  type CrearEmpleadoFormValues,
} from "../schemas/empleado";

const ETIQUETAS_TIPO_EXTRA: Record<TipoPagoHoraExtra, string> = {
  MULTIPLICADOR_HORA: "Multiplicador sobre hora base (ej: 1.5x)",
  MONTO_FIJO_HORA: "Monto fijo por hora ($)",
  POR_TRABAJO_REALIZADO: "Por trabajo / monto manual",
};

export function NuevoEmpleadoDialog() {
  const [open, setOpen] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);

  const cuentasQuery = useCuentas();
  const categoriasQuery = useCategorias();
  const metodosQuery = useMetodosPago();
  const crearEmpleado = useCrearEmpleado();

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
  } = useForm<CrearEmpleadoFormValues>({
    resolver: zodResolver(crearEmpleadoSchema),
    defaultValues: {
      nombre: "",
      apellido: "",
      dni: "",
      sueldoBasePesos: undefined,
      horasMensualesBase: 160,
      tipoHoraExtra: "MULTIPLICADOR_HORA",
      valorHoraExtra: 1.5,
      diaPagoSueldo: 10,
      diaPagoExtras: 20,
      cuentaOrigenId: 0,
      categoriaId: categoriaNominaDefault,
      metodoPagoId: 0,
    },
  });

  const tipoHoraExtra = watch("tipoHoraExtra");
  const cuentaOrigenId = watch("cuentaOrigenId");
  const categoriaId = watch("categoriaId");
  const metodoPagoId = watch("metodoPagoId");

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (!nextOpen) {
      setErrorGeneral(null);
      reset({
        nombre: "",
        apellido: "",
        dni: "",
        sueldoBasePesos: undefined,
        horasMensualesBase: 160,
        tipoHoraExtra: "MULTIPLICADOR_HORA",
        valorHoraExtra: 1.5,
        diaPagoSueldo: 10,
        diaPagoExtras: 20,
        cuentaOrigenId: 0,
        categoriaId: categoriaNominaDefault,
        metodoPagoId: 0,
      });
    } else if (categoriaId === 0 && categoriaNominaDefault > 0) {
      setValue("categoriaId", categoriaNominaDefault);
    }
  };

  const onSubmit = (values: CrearEmpleadoFormValues) => {
    setErrorGeneral(null);

    crearEmpleado.mutate(
      {
        nombre: values.nombre,
        apellido: values.apellido,
        dni: values.dni,
        sueldoBaseCentavos: pesosACentavos(values.sueldoBasePesos),
        horasMensualesBase: values.horasMensualesBase,
        tipoHoraExtra: values.tipoHoraExtra,
        valorHoraExtra:
          values.tipoHoraExtra === "POR_TRABAJO_REALIZADO"
            ? 0
            : values.valorHoraExtra,
        diaPagoSueldo: values.diaPagoSueldo,
        diaPagoExtras: values.diaPagoExtras,
        cuentaOrigenId: values.cuentaOrigenId,
        categoriaId: values.categoriaId,
        metodoPagoId: values.metodoPagoId,
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
      }
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
      <Button onClick={() => handleOpenChange(true)} className="h-11 gap-2">
        <UserPlus className="h-4 w-4" />
        Nuevo empleado
      </Button>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Alta de empleado</DialogTitle>
            <DialogDescription>
              Registrá los datos laborales y configurá el pago mensual
              automático en la agenda.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            {errorGeneral && (
              <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-sm text-destructive">
                {errorGeneral}
              </div>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="emp-nombre">Nombre</Label>
                <Input
                  id="emp-nombre"
                  placeholder="Ej: Juan"
                  className="h-11 text-base md:text-sm"
                  {...register("nombre")}
                />
                {errors.nombre && (
                  <p className="text-xs text-destructive">
                    {errors.nombre.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="emp-apellido">Apellido</Label>
                <Input
                  id="emp-apellido"
                  placeholder="Ej: Pérez"
                  className="h-11 text-base md:text-sm"
                  {...register("apellido")}
                />
                {errors.apellido && (
                  <p className="text-xs text-destructive">
                    {errors.apellido.message}
                  </p>
                )}
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
              <div className="space-y-2">
                <Label htmlFor="emp-dni">DNI / Documento</Label>
                <Input
                  id="emp-dni"
                  inputMode="numeric"
                  placeholder="Ej: 35123456"
                  className="h-11 text-base md:text-sm"
                  {...register("dni")}
                />
                {errors.dni && (
                  <p className="text-xs text-destructive">
                    {errors.dni.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="emp-sueldo">Sueldo base ($)</Label>
                <Input
                  id="emp-sueldo"
                  type="number"
                  step="0.01"
                  min="0.01"
                  inputMode="decimal"
                  placeholder="0.00"
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
                <Label htmlFor="emp-horas">Horas / mes</Label>
                <Input
                  id="emp-horas"
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
                      const nuevo = val as TipoPagoHoraExtra;
                      setValue("tipoHoraExtra", nuevo, { shouldValidate: true });
                      if (nuevo === "POR_TRABAJO_REALIZADO") {
                        setValue("valorHoraExtra", 0);
                      } else if (nuevo === "MULTIPLICADOR_HORA") {
                        setValue("valorHoraExtra", 1.5);
                      }
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
                  <Label htmlFor="emp-valor-extra">
                    {tipoHoraExtra === "MULTIPLICADOR_HORA"
                      ? "Multiplicador (ej: 1.5)"
                      : "Valor fijo por hora ($)"}
                  </Label>
                  <Input
                    id="emp-valor-extra"
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
                <Label htmlFor="emp-dia-sueldo">Día pago sueldo (1-31)</Label>
                <Input
                  id="emp-dia-sueldo"
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
                <Label htmlFor="emp-dia-extras">Día pago extras (1-31)</Label>
                <Input
                  id="emp-dia-extras"
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

            <div className="space-y-2 border-t pt-3">
              <p className="text-xs font-medium text-muted-foreground">
                Parámetros para la programación automática de sueldos
              </p>
              <div className="grid gap-4 sm:grid-cols-3">
                <div className="space-y-1.5">
                  <Label>Cuenta</Label>
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

                <div className="space-y-1.5">
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

                <div className="space-y-1.5">
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
                disabled={crearEmpleado.isPending}
              >
                {crearEmpleado.isPending ? "Guardando..." : "Registrar empleado"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
}