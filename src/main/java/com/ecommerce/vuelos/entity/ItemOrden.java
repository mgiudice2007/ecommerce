package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items_orden")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disponibilidad_id", nullable = false)
    private Disponibilidad disponibilidad;

    @Column(nullable = false)
    private Integer cantidad;

    /** Precio unitario ya con descuento aplicado, tomado al momento de la orden. */
    @Column(nullable = false)
    private BigDecimal precioUnitario;

    /**
     * Cuanto se descontro por asiento. Guardarlo aparte no es redundante:
     * con solo el precio final no habria forma de saber si hubo descuento
     * ni de cuanto fue, porque el descuento del vuelo puede cambiar despues.
     */
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal descuentoAplicado = BigDecimal.ZERO;
}
