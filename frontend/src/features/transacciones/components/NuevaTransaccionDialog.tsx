import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  ArrowDownLeft,
  ArrowLeftRight,
  ArrowUpRight,
  Plus,
} from "lucide-react";
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
import {
  useCrearGasto,
  useCrearIngreso,
  useCrearTransferencia,
} from "../hooks/useTransacciones";
import {
  nuevaTransaccionSchema,
  type NuevaTransaccionFormValues,
  type TipoOperacionForm,
} from "../schemas/transaccion";

function obtenerFechaHoyISO(): string {
  const hoy = new Date();
  const anio = hoy.getFullYear();
  const mes = String(hoy.getMonth() + 1).padStart(2, "0");
  const dia = String(hoy.getDate()).padStart(2, "0");
  return `${anio}-${mes}-${dia}`;
}

export function NuevaTransaccionDialog() {
  const [open, setOpen] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);

  const cuentasQuery = useCuentas();
  const categoriasQuery = useCategorias();
  const metodosQuery = useMetodosPago();

  const crearGasto = useCrearGasto();
  const crearIngreso = useCrearIngreso();
  const crearTransferencia = useCrearTransferencia();

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
  } = useForm<NuevaTransaccionFormValues>({
    resolver: zodResolver(nuevaTransaccionSchema),
    defaultValues: {
      tipoMovimiento: "GASTO",
      montoPesos: undefined,
      cuentaOrigenId: 0,
      cuentaDestinoId: 0,
      categoriaId: 0,
      metodoPagoId: 0,
      fecha: obtenerFechaHoyISO(),
      descripcion: "",
      tercero: "",
      garantiaMeses: undefined,
    },
  });

  const tipoMovimiento = watch("tipoMovimiento");
  const cuentaOrigenId = watch("cuentaOrigenId");
  const cuentaDestinoId = watch("cuentaDestinoId");
  const categoriaId = watch("categoriaId");
  const metodoPagoId = watch("metodoPagoId");

  const isPending =
    crearGasto.isPending ||
    crearIngreso.isPending ||
    crearTransferencia.isPending;

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (!nextOpen) {
      setErrorGeneral(null);
      reset({
        tipoMovimiento: "GASTO",
        montoPesos: undefined,
        cuentaOrigenId: 0,
        cuentaDestinoId: 0,
        categoriaId: 0,
        metodoPagoId: 0,
        fecha: obtenerFechaHoyISO(),
        descripcion: "",
        tercero: "",
        garantiaMeses: undefined,
      });
    }
  };

  const cambiarTipo = (nuevoTipo: TipoOperacionForm) => {
    setErrorGeneral(null);
    setValue("tipoMovimiento", nuevoTipo, { shouldValidate: false });
  };

  const onSubmit = (values: NuevaTransaccionFormValues) => {
    setErrorGeneral(null);

    const basePayload = {
      montoCentavos: pesosACentavos(values.montoPesos),
      cuentaOrigenId: values.cuentaOrigenId,
      metodoPagoId: values.metodoPagoId,
      fecha: values.fecha ? values.fecha : undefined,
      descripcion: values.descripcion ? values.descripcion : undefined,
      tercero: values.tercero ? values.tercero : undefined,
      garantiaMeses: values.garantiaMeses,
    };

    const callbacks = {
      onSuccess: () => {
        handleOpenChange(false);
      },
      onError: (err: unknown) => {
        const hayErroresCampo = aplicarErroresCampos(err, setError);
        if (!hayErroresCampo) {
          setErrorGeneral(obtenerMensajeError(err));
        }
      },
    };

    if (values.tipoMovimiento === "GASTO") {
      crearGasto.mutate(
        { ...basePayload, categoriaId: values.categoriaId },
        callbacks,
      );
      return;
    }

    if (values.tipoMovimiento === "INGRESO") {
      crearIngreso.mutate(
        { ...basePayload, categoriaId: values.categoriaId },
        callbacks,
      );
      return;
    }

    crearTransferencia.mutate(
      { ...basePayload, cuentaDestinoId: values.cuentaDestinoId },
      callbacks,
    );
  };

  const cuentaOrigenNombre =
    cuentas.find((c) => c.id === cuentaOrigenId)?.nombre ??
    "Seleccioná una cuenta";
  const cuentaDestinoNombre =
    cuentas.find((c) => c.id === cuentaDestinoId)?.nombre ??
    "Seleccioná cuenta destino";
  const categoriaNombre =
    categorias.find((c) => c.id === categoriaId)?.nombre ??
    "Seleccioná una categoría";
  const metodoNombre =
    metodos.find((m) => m.id === metodoPagoId)?.nombre ??
    "Seleccioná un método";

  return (
    <>
      <Button onClick={() => setOpen(true)} className="h-11 gap-2">
        <Plus className="h-4 w-4" />
        Nuevo movimiento
      </Button>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent className="max-h-[90dvh] overflow-y-auto sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Registrar movimiento</DialogTitle>
            <DialogDescription>
              Cargá un gasto, ingreso o transferencia entre tus cuentas.
            </DialogDescription>
          </DialogHeader>

          <div className="grid grid-cols-3 gap-1.5 rounded-lg bg-muted p-1">
            <Button
              type="button"
              variant={tipoMovimiento === "GASTO" ? "default" : "ghost"}
              className="h-9 gap-1.5 text-xs sm:text-sm"
              onClick={() => cambiarTipo("GASTO")}
            >
              <ArrowUpRight className="h-4 w-4" />
              Gasto
            </Button>
            <Button
              type="button"
              variant={tipoMovimiento === "INGRESO" ? "default" : "ghost"}
              className="h-9 gap-1.5 text-xs sm:text-sm"
              onClick={() => cambiarTipo("INGRESO")}
            >
              <ArrowDownLeft className="h-4 w-4" />
              Ingreso
            </Button>
            <Button
              type="button"
              variant={tipoMovimiento === "TRANSFERENCIA" ? "default" : "ghost"}
              className="h-9 gap-1.5 text-xs sm:text-sm"
              onClick={() => cambiarTipo("TRANSFERENCIA")}
            >
              <ArrowLeftRight className="h-4 w-4" />
              Transferencia
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

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="tx-monto">Monto ($)</Label>
                <Input
                  id="tx-monto"
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
                <Label htmlFor="tx-fecha">Fecha</Label>
                <Input
                  id="tx-fecha"
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

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>
                  {tipoMovimiento === "TRANSFERENCIA"
                    ? "Cuenta origen"
                    : tipoMovimiento === "INGRESO"
                      ? "Cuenta destino"
                      : "Cuenta"}
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
                    <SelectValue>{cuentaOrigenNombre}</SelectValue>
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

              {tipoMovimiento === "TRANSFERENCIA" ? (
                <div className="space-y-2">
                  <Label>Cuenta destino</Label>
                  <Select
                    value={cuentaDestinoId > 0 ? String(cuentaDestinoId) : null}
                    onValueChange={(val: string | null) => {
                      setValue("cuentaDestinoId", val ? Number(val) : 0, {
                        shouldValidate: true,
                      });
                    }}
                  >
                    <SelectTrigger className="h-11 w-full text-base md:text-sm">
                      <SelectValue>{cuentaDestinoNombre}</SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {cuentas
                        .filter((c) => c.id !== cuentaOrigenId)
                        .map((cuenta) => (
                          <SelectItem key={cuenta.id} value={String(cuenta.id)}>
                            {cuenta.nombre}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                  {errors.cuentaDestinoId && (
                    <p className="text-xs text-destructive">
                      {errors.cuentaDestinoId.message}
                    </p>
                  )}
                </div>
              ) : (
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
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
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

              <div className="space-y-2">
                <Label htmlFor="tx-tercero">
                  Comercio / Tercero (opcional)
                </Label>
                <Input
                  id="tx-tercero"
                  placeholder="Ej: Supermercado, Cliente..."
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

            <div className="grid gap-4 sm:grid-cols-3">
              <div
                className={
                  tipoMovimiento === "GASTO"
                    ? "space-y-2 sm:col-span-2"
                    : "space-y-2 sm:col-span-3"
                }
              >
                <Label htmlFor="tx-descripcion">Descripción (opcional)</Label>
                <Input
                  id="tx-descripcion"
                  placeholder="Detalle del movimiento"
                  className="h-11 text-base md:text-sm"
                  {...register("descripcion")}
                />
                {errors.descripcion && (
                  <p className="text-xs text-destructive">
                    {errors.descripcion.message}
                  </p>
                )}
              </div>

              {tipoMovimiento === "GASTO" && (
                <div className="space-y-2">
                  <Label htmlFor="tx-garantia">Garantía (meses)</Label>
                  <Input
                    id="tx-garantia"
                    type="number"
                    min="1"
                    max="120"
                    inputMode="numeric"
                    placeholder="Opcional"
                    className="h-11 text-base md:text-sm"
                    {...register("garantiaMeses", {
                      setValueAs: (val: string) =>
                        val === "" ? undefined : Number(val),
                    })}
                  />
                  {errors.garantiaMeses && (
                    <p className="text-xs text-destructive">
                      {errors.garantiaMeses.message}
                    </p>
                  )}
                </div>
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
              <Button type="submit" className="h-11" disabled={isPending}>
                {isPending ? "Guardando..." : "Guardar movimiento"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
}
