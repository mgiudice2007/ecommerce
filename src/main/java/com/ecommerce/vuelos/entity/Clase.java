package com.ecommerce.vuelos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean equipajeBodega = false;

    /** Cuantas veces se multiplican las millas al comprar en esta clase (Economica 1, Ejecutiva 2...). */
    @Column
    private Integer multiplicadorMillas;

    /** Asientos por fila en el mapa del avion (Economica 6, Ejecutiva 4, Primera 2). */
    @Column
    private Integer asientosPorFila;

    /** Cuantos asientos puede tener esta clase en un vuelo, como en un avion real (Economica 250, Ejecutiva 40, Primera 16). */
    @Column
    private Integer maximoAsientos;

    /** Cuantos asientos tiene que tener como minimo (Economica 50, Ejecutiva 8, Primera 4). */
    @Column
    private Integer minimoAsientos;

    /**
     * De la mas basica a la mejor (Economica 1, Ejecutiva 2, Primera 3). Sirve para que
     * en un vuelo una clase mejor no cueste menos que una mas basica.
     */
    @Column
    private Integer nivel;
}
