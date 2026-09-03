package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "aerolineas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aerolinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @OneToMany(mappedBy = "aerolinea", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Vuelo> vuelos = new ArrayList<>();
}
