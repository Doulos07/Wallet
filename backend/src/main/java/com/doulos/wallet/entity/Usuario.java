package com.doulos.wallet.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Usuario implements UserDetails {
    // con UserDatils: Se convierte tambien en un Objeto de Seguridad

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Builder.Default
    private boolean estado = true;

    @Builder.Default
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cuenta> cuentas = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "jefe",  cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Empleado> empleados = new ArrayList<>();

    // ==============================
    // UserDatils
    // ==============================
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
       // return UserDetails.super.isAccountNonExpired();
        return true; // No usamos expiración de cuentas
    }

    @Override
    public boolean isAccountNonLocked() {
        // return UserDetails.super.isAccountNonLocked();
        return true; // No usamos bloqueo de cuentas
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // return UserDetails.super.isCredentialsNonExpired();
        return true; // Las contraseñas no caducan
    }

    @Override
    public boolean isEnabled() {
        // return UserDetails.super.isEnabled();
        return this.estado; // Spring Security respeta el estado activo/inactivo del usuario
    }
}
