package com.doulos.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity // Le dice a Spring que esto es una tabla en la base de datos
@Table(name = "metodos_pago")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MetodoPago {

    @Id // Llave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Autoincremental
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Builder.Default
    @Column(nullable = false)
    private boolean estado = true; // Por defecto activo para el soft-delete
}


