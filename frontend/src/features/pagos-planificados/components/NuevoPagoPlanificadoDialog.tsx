import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowDownLeft, ArrowUpRight, CalendarPlus } from "lucide-react";
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
import type { FrecuenciaPago, TipoRecurrenciaPago } from "@/types/enums";
import type { CrearPagoPlanificadoDTO } from "@/types/pagosPlanificados";
import type { TransaccionPlanificableDTO } from "@/types/transacciones";
import { useCrearPagoPlanificado } from "../hooks/usePagosPlanificados";
import {
  FRECUENCIAS_PAGO,
  nuevoPagoPlanificadoSchema,
  TIPOS_RECURRENCIA,
  type NuevoPagoPlanificadoFormValues,
} from "../schemas/pagoPlanificado";

const ETIQUETAS_RECURRENCIA: Record<TipoRecurrenciaPago, string> = {
  UNICO: "Pago único (una sola vez)",
  INFINITO: "Recurrente indefinido",
  HASTA_FECHA: "Recurrente hasta una fecha",
  POR_EVENTO: "Por cantidad de cuotas / eventos",
};

const ETIQUETAS_FRECUENCIA: Record<FrecuenciaPago, string> = {
  DAYS: "Diaria",
  WEEKS: "Semanal",
  MONTHS: "Mensual",
  YEARS: "Anual",
};

function obtenerFechaHoyISO(): string {
  const hoy = new Date();
  const anio = hoy.getFullYear();
  const mes = String(hoy.getMonth() + 1).padStart(2, "0");
  const dia = String(hoy.getDate()).padStart(2, "0");
  return `${anio}-${mes}-${dia}`;
}

export function NuevoPagoPlanificadoDialog() {
  const [open, setOpen] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);

  const cuentasQuery = useCuentas();
  const categoriasQuery = useCategorias();
  const metodosQuery = useMetodosPago();
  const crearPago = useCrearPagoPlanificado();

  const cuentas = Array.isArray(cuentasQuery.data)
    ? cuentasQuery.data.filter((c) => c.tipo !== "EXTERNA")
    : [];
  const categorias = Array.isArray(categoriasQuery.data)
    ? categoriasQuery.data
    : [];
  const metodos = Array.isArray(metodosQuery.data) ? metodosQuery.data : [];

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    setError,
    formState: { errors },
  } = useForm<NuevoPagoPlanificadoFormValues>({
    resolver: zodResolver(nuevoPagoPlanificadoSchema),
    defaultValues: {
      tipoMovimiento: "GASTO",
      tipoRecurrencia: "UNICO",
      fecha: obtenerFechaHoyISO(),
      frecuencia: "MONTHS",
      fechaFin: "",
      cantidadEventos: undefined,
      montoPesos: undefined,
      cuentaOrigenId: 0,
      categoriaId: 0,
      metodoPagoId: 0,
      descripcion: "",
      tercero: "",
    },
  });

  const tipoMovimiento = watch("tipoMovimiento");
  const tipoRecurrencia = watch("tipoRecurrencia");
  const frecuencia = watch("frecuencia");
  const cuentaOrigenId = watch("cuentaOrigenId");
  const categoriaId = watch("categoriaId");
  const metodoPagoId = watch("metodoPagoId");

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (!nextOpen) {
      setErrorGeneral(null);
      reset({
        tipoMovimiento: "GASTO",
        tipoRecurrencia: "UNICO",
        fecha: obtenerFechaHoyISO(),
        frecuencia: "MONTHS",
        fechaFin: "",
        cantidadEventos: undefined,
        montoPesos: undefined,
        cuentaOrigenId: 0,
        categoriaId: 0,
        metodoPagoId: 0,
        descripcion: "",
        tercero: "",
      });
    }
  };

  const construirPayload = (
    values: NuevoPagoPlanificadoFormValues,
  ): CrearPagoPlanificadoDTO => {
    const plantillaBase = {
      montoCentavos: pesosACentavos(values.montoPesos),
      cuentaOrigenId: values.cuentaOrigenId,
      categoriaId: values.categoriaId,
      metodoPagoId: values.metodoPagoId,
      descripcion: values.descripcion ? values.descripcion : undefined,
      tercero: values.tercero ? values.tercero : undefined,
    };

    const plantilla: TransaccionPlanificableDTO =
      values.tipoMovimiento === "GASTO"
        ? { ...plantillaBase, tipoMovimiento: "GASTO" }
        : { ...plantillaBase, tipoMovimiento: "INGRESO" };

    switch (values.tipoRecurrencia) {
      case "UNICO":
        return {
          tipoRecurrencia: "UNICO",
          fecha: values.fecha,
          plantilla,
        };
      case "INFINITO":
        return {
          tipoRecurrencia: "INFINITO",
          fecha: values.fecha,
          frecuencia: values.frecuencia,
          plantilla,
        };
      case "HASTA_FECHA":
        return {
          tipoRecurrencia: "HASTA_FECHA",
          fecha: values.fecha,
          frecuencia: values.frecuencia,
          fechaFin: values.fechaFin ?? "",
          plantilla,
        };
      case "POR_EVENTO":
        return {
          tipoRecurrencia: "POR_EVENTO",
          fecha: values.fecha,
          frecuencia: values.frecuencia,
          cantidadEventos: values.cantidadEventos ?? 1,
          plantilla,
        };
    }
  };

  const onSubmit = (values: NuevoPagoPlanificadoFormValues) => {
    setErrorGeneral(null);
    const payload = construirPayload(values);

    crearPago.mutate(payload, {
      onSuccess: () => {
        handleOpenChange(false);
      },
      onError: (err: unknown) => {
        const hayErroresCampo = aplicarErroresCampos(err, setError);
        if (!hayErroresCampo) {
          setErrorGeneral(obtenerMensajeError(err));
        }
      },
    });
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
      <Button onClick={() => setOpen(true)} className="h-11 gap-2">
        <CalendarPlus className="h-4 w-4" />
        Programar pago
      </Button>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Nuevo pago planificado</DialogTitle>
            <DialogDescription>
              Programá un gasto o ingreso único o recurrente para su ejecución
              automática.
            </DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-2 gap-1.5 rounded-lg bg-muted p-1">
            <Button
              type="button"
              variant={tipoMovimiento === "GASTO" ? "default" : "ghost"}
              className="h-9 gap-1.5 text-xs sm:text-sm"
              onClick={() => setValue("tipoMovimiento", "GASTO")}
            >
              <ArrowUpRight className="h-4 w-4" />
              Gasto programado
            </Button>
            <Button
              type="button"
              variant={tipoMovimiento === "INGRESO" ? "default" : "ghost"}
              className="h-9 gap-1.5 text-xs sm:text-sm"
              onClick={() => setValue("tipoMovimiento", "INGRESO")}
            >
              <ArrowDownLeft className="h-4 w-4" />
              Ingreso programado
            </Button>
          </div>

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

            <div className="space-y-2">
              <Label>Modalidad de recurrencia</Label>
              <Select
                value={tipoRecurrencia}
                onValueChange={(val: string | null) => {
                  if (val) {
                    setValue("tipoRecurrencia", val as TipoRecurrenciaPago, {
                      shouldValidate: true,
                    });
                  }
                }}
              >
                <SelectTrigger className="h-11 w-full text-base md:text-sm">
                  <SelectValue>
                    {ETIQUETAS_RECURRENCIA[tipoRecurrencia]}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {TIPOS_RECURRENCIA.map((tipo) => (
                    <SelectItem key={tipo} value={tipo}>
                      {ETIQUETAS_RECURRENCIA[tipo]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="pp-fecha">
                  {tipoRecurrencia === "UNICO"
                    ? "Fecha de ejecución"
                    : "Primera ejecución"}
                </Label>
                <Input
                  id="pp-fecha"
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

              {tipoRecurrencia !== "UNICO" && (
                <div className="space-y-2">
                  <Label>Frecuencia</Label>
                  <Select
                    value={frecuencia}
                    onValueChange={(val: string | null) => {
                      if (val) {
                        setValue("frecuencia", val as FrecuenciaPago, {
                          shouldValidate: true,
                        });
                      }
                    }}
                  >
                    <SelectTrigger className="h-11 w-full text-base md:text-sm">
                      <SelectValue>
                        {ETIQUETAS_FRECUENCIA[frecuencia]}
                      </SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {FRECUENCIAS_PAGO.map((freq) => (
                        <SelectItem key={freq} value={freq}>
                          {ETIQUETAS_FRECUENCIA[freq]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              )}
            </div>

            {tipoRecurrencia === "HASTA_FECHA" && (
              <div className="space-y-2">
                <Label htmlFor="pp-fecha-fin">Fecha de finalización</Label>
                <Input
                  id="pp-fecha-fin"
                  type="date"
                  className="h-11 text-base md:text-sm"
                  {...register("fechaFin")}
                />
                {errors.fechaFin && (
                  <p className="text-xs text-destructive">
                    {errors.fechaFin.message}
                  </p>
                )}
              </div>
            )}

            {tipoRecurrencia === "POR_EVENTO" && (
              <div className="space-y-2">
                <Label htmlFor="pp-eventos">
                  Cantidad de repeticiones / cuotas
                </Label>
                <Input
                  id="pp-eventos"
                  type="number"
                  min="1"
                  inputMode="numeric"
                  placeholder="Ej: 12"
                  className="h-11 text-base md:text-sm"
                  {...register("cantidadEventos", {
                    setValueAs: (val: string) =>
                      val === "" ? undefined : Number(val),
                  })}
                />
                {errors.cantidadEventos && (
                  <p className="text-xs text-destructive">
                    {errors.cantidadEventos.message}
                  </p>
                )}
              </div>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="pp-monto">Monto ($)</Label>
                <Input
                  id="pp-monto"
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
                <Label>
                  {tipoMovimiento === "INGRESO" ? "Cuenta destino" : "Cuenta"}
                </Label>
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

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="pp-descripcion">Concepto / Descripción</Label>
                <Input
                  id="pp-descripcion"
                  placeholder="Ej: Alquiler, Internet, Sueldo..."
                  className="h-11 text-base md:text-sm"
                  {...register("descripcion")}
                />
                {errors.descripcion && (
                  <p className="text-xs text-destructive">
                    {errors.descripcion.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="pp-tercero">
                  Comercio / Tercero (opcional)
                </Label>
                <Input
                  id="pp-tercero"
                  placeholder="Ej: Movistar, Inmobiliaria..."
                  className="h-11 text-base md:text-sm"
                  {...register("tercero")}
                />
                {errors.tercero && (
                  <p className="text-xs text-destructive">
                    {errors.tercero.message}
                  </p>
                )}
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
                disabled={crearPago.isPending}
              >
                {crearPago.isPending ? "Guardando..." : "Guardar programación"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
}
