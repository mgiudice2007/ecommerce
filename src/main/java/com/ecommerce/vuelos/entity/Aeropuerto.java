package com.ecommerce.vuelos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aeropuertos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aeropuerto {

    /** Clave natural: el codigo IATA de tres letras (EZE, MAD, MIA). */
    @Id
    @Column(length = 3)
    private String codigoIata;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String ciudad;

    @Column(nullable = false)
    private String provincia;

    @Column(nullable = false)
    private String pais;

    @Column
    private String zonaHoraria;
}
