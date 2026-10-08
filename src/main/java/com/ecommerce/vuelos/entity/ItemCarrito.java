package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "items_carrito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disponibilidad_id", nullable = false)
    private Disponibilidad disponibilidad;

    @Column(nullable = false)
    private Integer cantidad;

    /** Adulto, nino o bebe. Los items viejos (antes de existir el campo) cuentan como adulto. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TipoPasajero tipoPasajero;

    public TipoPasajero getTipoPasajero() {
        return tipoPasajero == null ? TipoPasajero.ADULTO : tipoPasajero;
    }
}
