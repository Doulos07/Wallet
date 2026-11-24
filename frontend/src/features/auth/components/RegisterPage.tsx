import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, Navigate } from "react-router-dom";
import { Wallet } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { aplicarErroresCampos, obtenerMensajeError } from "@/lib/errors";
import { useAuth } from "../hooks/useAuth";
import { registerSchema, type RegisterFormValues } from "../schemas/auth";

export function RegisterPage() {
  const { isAuthenticated, registerMutation } = useAuth();
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { nombre: "", email: "", password: "" },
  });

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const onSubmit = (values: RegisterFormValues) => {
    setErrorGeneral(null);
    registerMutation.mutate(values, {
      onError: (err) => {
        const hayErroresCampo = aplicarErroresCampos(err, setError);
        if (!hayErroresCampo) {
          setErrorGeneral(obtenerMensajeError(err));
        }
      },
    });
  };

  return (
    <main className="flex min-h-dvh flex-col justify-center bg-muted/40 px-4 py-8 sm:items-center">
      <Card className="w-full max-w-md border-border/80 shadow-sm">
        <CardHeader className="space-y-2 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
            <Wallet className="h-6 w-6" />
          </div>
          <CardTitle className="text-2xl font-bold tracking-tight">
            Crear cuenta
          </CardTitle>
          <CardDescription>
            Configurá tu billetera personal en segundos
          </CardDescription>
        </CardHeader>

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <CardContent className="space-y-4">
            {errorGeneral && (
              <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-sm text-destructive">
                {errorGeneral}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="nombre">Nombre</Label>
              <Input
                id="nombre"
                type="text"
                autoComplete="name"
                placeholder="Santiago"
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
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                inputMode="email"
                autoComplete="email"
                placeholder="nombre@ejemplo.com"
                className="h-11 text-base md:text-sm"
                {...register("email")}
              />
              {errors.email && (
                <p className="text-xs text-destructive">
                  {errors.email.message}
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Contraseña</Label>
              <Input
                id="password"
                type="password"
                autoComplete="new-password"
                placeholder="Mínimo 6 caracteres"
                className="h-11 text-base md:text-sm"
                {...register("password")}
              />
              {errors.password && (
                <p className="text-xs text-destructive">
                  {errors.password.message}
                </p>
              )}
            </div>
          </CardContent>

          <CardFooter className="flex flex-col gap-4 pt-2">
            <Button
              type="submit"
              className="h-11 w-full text-base font-medium md:text-sm"
              disabled={registerMutation.isPending}
            >
              {registerMutation.isPending ? "Creando cuenta..." : "Registrarme"}
            </Button>

            <p className="text-center text-sm text-muted-foreground">
              ¿Ya tenés cuenta?{" "}
              <Link
                to="/login"
                className="font-medium text-primary underline-offset-4 hover:underline"
              >
                Iniciá sesión
              </Link>
            </p>
          </CardFooter>
        </form>
      </Card>
    </main>
  );
}
