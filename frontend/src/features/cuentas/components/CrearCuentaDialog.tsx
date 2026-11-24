import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus } from "lucide-react";
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
import { ETIQUETAS_TIPO_CUENTA, type TipoCuentaUsuario } from "@/types/cuentas";
import { useCrearCuenta } from "../hooks/useCuentas";
import {
  crearCuentaSchema,
  type CrearCuentaFormValues,
} from "../schemas/cuenta";

const OPCIONES_TIPO: ReadonlyArray<[TipoCuentaUsuario, string]> =
  Object.entries(ETIQUETAS_TIPO_CUENTA) as Array<[TipoCuentaUsuario, string]>;

function esTipoCuentaUsuario(valor: string | null): valor is TipoCuentaUsuario {
  return valor !== null && valor in ETIQUETAS_TIPO_CUENTA;
}

export function CrearCuentaDialog() {
  const [open, setOpen] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const crearCuenta = useCrearCuenta();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    setError,
    formState: { errors },
  } = useForm<CrearCuentaFormValues>({
    resolver: zodResolver(crearCuentaSchema),
    defaultValues: {
      nombre: "",
      tipo: "CUENTA_GENERAL",
      montoInicialPesos: 0,
    },
  });

  const tipoSeleccionado = watch("tipo");

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);
    if (!nextOpen) {
      setErrorGeneral(null);
      reset();
    }
  };

  const onSubmit = (values: CrearCuentaFormValues) => {
    setErrorGeneral(null);
    crearCuenta.mutate(
      {
        nombre: values.nombre,
        tipo: values.tipo,
        montoInicialCentavos: pesosACentavos(values.montoInicialPesos),
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
      <Button onClick={() => setOpen(true)} className="h-11 gap-2">
        <Plus className="h-4 w-4" />
        Nueva cuenta
      </Button>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Nueva cuenta</DialogTitle>
            <DialogDescription>
              Agregá una billetera, cuenta bancaria o efectivo para registrar
              tus movimientos.
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

            <div className="space-y-2">
              <Label htmlFor="cuenta-nombre">Nombre</Label>
              <Input
                id="cuenta-nombre"
                placeholder="Ej: Banco Galicia, Efectivo, Mercado Pago"
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
              <Label>Tipo de cuenta</Label>
              <Select
                value={tipoSeleccionado}
                onValueChange={(val: string | null) => {
                  if (esTipoCuentaUsuario(val)) {
                    setValue("tipo", val, { shouldValidate: true });
                  }
                }}
              >
                <SelectTrigger className="h-11 w-full text-base md:text-sm">
                  <SelectValue placeholder="Seleccioná un tipo">
                    {ETIQUETAS_TIPO_CUENTA[tipoSeleccionado]}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {OPCIONES_TIPO.map(([valor, etiqueta]) => (
                    <SelectItem key={valor} value={valor}>
                      {etiqueta}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.tipo && (
                <p className="text-xs text-destructive">
                  {errors.tipo.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="cuenta-saldo">Saldo inicial ($)</Label>
              <Input
                id="cuenta-saldo"
                type="number"
                step="0.01"
                min="0"
                inputMode="decimal"
                placeholder="0.00"
                className="h-11 text-base md:text-sm"
                {...register("montoInicialPesos", { valueAsNumber: true })}
              />
              {errors.montoInicialPesos && (
                <p className="text-xs text-destructive">
                  {errors.montoInicialPesos.message}
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
                disabled={crearCuenta.isPending}
              >
                {crearCuenta.isPending ? "Guardando..." : "Crear cuenta"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  );
}
