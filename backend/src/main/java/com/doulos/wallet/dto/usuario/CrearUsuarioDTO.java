package com.doulos.wallet.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearUsuarioDTO(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
        String nombre,

        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "El formato del email no es válido.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres.")
        String password
) {
}