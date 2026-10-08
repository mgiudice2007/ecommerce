package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

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

    /** Adulto, niño o bebé. Los items viejos (antes de existir el campo) cuentan como adulto. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TipoPasajero tipoPasajero;

    public TipoPasajero getTipoPasajero() {
        return tipoPasajero == null ? TipoPasajero.ADULTO : tipoPasajero;
    }

    @Column(nullable = false)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal descuentoAplicado = BigDecimal.ZERO;
}
