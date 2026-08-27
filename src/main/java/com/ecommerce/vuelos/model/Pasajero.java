package com.ecommerce.vuelos.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pasajeros")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Pasajero extends Usuario {

    @OneToOne(mappedBy = "pasajero", cascade = CascadeType.ALL, orphanRemoval = true)
    private Carrito carrito;

    @OneToMany(mappedBy = "pasajero", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Reserva> reservas = new ArrayList<>();
}
