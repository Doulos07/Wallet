package com.doulos.wallet.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "categoria")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Builder.Default
    @Column(nullable = false)
    private  boolean estado = true;

}
