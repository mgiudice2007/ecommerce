package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "administradores")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Administrador extends Usuario {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "administrador_permisos", joinColumns = @JoinColumn(name = "administrador_id"))
    @Column(name = "permiso")
    @Builder.Default
    private Set<String> permisos = new HashSet<>();
}
