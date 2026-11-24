import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { CreditCard, Plus, Tag, Trash2 } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { aplicarErroresCampos, obtenerMensajeError } from "@/lib/errors";
import {
  useCategorias,
  useCrearCategoria,
  useCrearMetodoPago,
  useDesactivarCategoria,
  useDesactivarMetodoPago,
  useMetodosPago,
} from "../hooks/useCatalogos";
import {
  crearCatalogoSchema,
  type CrearCatalogoFormValues,
} from "../schemas/catalogo";

interface ItemCatalogo {
  id: number;
  nombre: string;
}

interface SeccionCatalogoProps {
  titulo: string;
  subtitulo: string;
  placeholder: string;
  etiquetaSingular: string;
  icono: LucideIcon;
  items: ItemCatalogo[];
  isLoading: boolean;
  isError: boolean;
  error: unknown;
  isCreando: boolean;
  isDesactivando: boolean;
  onCrear: (
    nombre: string,
    callbacks: { onSuccess: () => void; onError: (err: unknown) => void },
  ) => void;
  onDesactivar: (
    id: number,
    callbacks: { onError: (err: unknown) => void },
  ) => void;
}

function SeccionCatalogo({
  titulo,
  subtitulo,
  placeholder,
  etiquetaSingular,
  icono: Icono,
  items,
  isLoading,
  isError,
  error,
  isCreando,
  isDesactivando,
  onCrear,
  onDesactivar,
}: SeccionCatalogoProps) {
  const [errorAccion, setErrorAccion] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<CrearCatalogoFormValues>({
    resolver: zodResolver(crearCatalogoSchema),
    defaultValues: { nombre: "" },
  });

  const onSubmit = (values: CrearCatalogoFormValues) => {
    setErrorAccion(null);
    onCrear(values.nombre, {
      onSuccess: () => {
        reset();
      },
      onError: (err: unknown) => {
        const hayErroresCampo = aplicarErroresCampos(err, setError);
        if (!hayErroresCampo) {
          setErrorAccion(obtenerMensajeError(err));
        }
      },
    });
  };

  const handleDesactivar = (item: ItemCatalogo) => {
    const confirmado = window.confirm(
      `¿Desactivar ${etiquetaSingular} "${item.nombre}"?`,
    );
    if (!confirmado) return;

    setErrorAccion(null);
    onDesactivar(item.id, {
      onError: (err: unknown) => {
        setErrorAccion(obtenerMensajeError(err));
      },
    });
  };

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <Icono className="h-5 w-5" />
          </div>
          <div>
            <CardTitle className="text-lg">{titulo}</CardTitle>
            <p className="text-xs text-muted-foreground">{subtitulo}</p>
          </div>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="flex flex-col gap-2 sm:flex-row"
          noValidate
        >
          <div className="flex-1 space-y-1">
            <Input
              placeholder={placeholder}
              className="h-11 text-base md:text-sm"
              {...register("nombre")}
            />
            {errors.nombre && (
              <p className="text-xs text-destructive">
                {errors.nombre.message}
              </p>
            )}
          </div>
          <Button
            type="submit"
            className="h-11 shrink-0 gap-1.5"
            disabled={isCreando}
          >
            <Plus className="h-4 w-4" />
            {isCreando ? "Agregando..." : "Agregar"}
          </Button>
        </form>

        {errorAccion && (
          <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive">
            {errorAccion}
          </div>
        )}

        {isError && (
          <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive">
            {obtenerMensajeError(error)}
          </div>
        )}

        {isLoading ? (
          <p className="py-4 text-center text-sm text-muted-foreground">
            Cargando...
          </p>
        ) : items.length === 0 ? (
          <p className="rounded-lg border border-dashed py-6 text-center text-sm text-muted-foreground">
            No hay elementos activos.
          </p>
        ) : (
          <ul className="divide-y rounded-lg border">
            {items.map((item) => (
              <li
                key={item.id}
                className="flex items-center justify-between px-3.5 py-2.5"
              >
                <span className="text-sm font-medium">{item.nombre}</span>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-9 w-9 text-muted-foreground hover:text-destructive"
                  onClick={() => handleDesactivar(item)}
                  disabled={isDesactivando}
                  aria-label={`Desactivar ${item.nombre}`}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

export function CatalogosPage() {
  const categoriasQuery = useCategorias();
  const crearCategoria = useCrearCategoria();
  const desactivarCategoria = useDesactivarCategoria();

  const metodosQuery = useMetodosPago();
  const crearMetodo = useCrearMetodoPago();
  const desactivarMetodo = useDesactivarMetodoPago();

  const categorias = Array.isArray(categoriasQuery.data)
    ? categoriasQuery.data
    : [];
  const metodos = Array.isArray(metodosQuery.data) ? metodosQuery.data : [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Catálogos</h1>
        <p className="text-sm text-muted-foreground">
          Administrá las categorías y medios de pago disponibles para tus
          movimientos
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <SeccionCatalogo
          titulo="Categorías"
          subtitulo="Clasificación de gastos e ingresos"
          placeholder="Ej: Mascotas, Suscripciones..."
          etiquetaSingular="la categoría"
          icono={Tag}
          items={categorias}
          isLoading={categoriasQuery.isLoading}
          isError={categoriasQuery.isError}
          error={categoriasQuery.error}
          isCreando={crearCategoria.isPending}
          isDesactivando={desactivarCategoria.isPending}
          onCrear={(nombre, callbacks) =>
            crearCategoria.mutate({ nombre }, callbacks)
          }
          onDesactivar={(id, callbacks) =>
            desactivarCategoria.mutate(id, callbacks)
          }
        />

        <SeccionCatalogo
          titulo="Métodos de pago"
          subtitulo="Canales o instrumentos de cobro y pago"
          placeholder="Ej: Mercado Pago, Modo, Cheque..."
          etiquetaSingular="el método de pago"
          icono={CreditCard}
          items={metodos}
          isLoading={metodosQuery.isLoading}
          isError={metodosQuery.isError}
          error={metodosQuery.error}
          isCreando={crearMetodo.isPending}
          isDesactivando={desactivarMetodo.isPending}
          onCrear={(nombre, callbacks) =>
            crearMetodo.mutate({ nombre }, callbacks)
          }
          onDesactivar={(id, callbacks) =>
            desactivarMetodo.mutate(id, callbacks)
          }
        />
      </div>
    </div>
  );
}
