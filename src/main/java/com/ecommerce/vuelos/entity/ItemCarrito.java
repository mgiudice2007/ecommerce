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

    /** Asientos elegidos para estos pasajeros ("12A", "12B"...). Se venden recien al confirmar la compra. */
    @ElementCollection
    @CollectionTable(name = "items_carrito_asientos", joinColumns = @JoinColumn(name = "item_carrito_id"))
    @Column(name = "codigo", length = 5)
    @Builder.Default
    private List<String> asientos = new ArrayList<>();

    /** Adulto, niño o bebé. Los items viejos (antes de existir el campo) cuentan como adulto. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TipoPasajero tipoPasajero;

    public TipoPasajero getTipoPasajero() {
        return tipoPasajero == null ? TipoPasajero.ADULTO : tipoPasajero;
    }
}
