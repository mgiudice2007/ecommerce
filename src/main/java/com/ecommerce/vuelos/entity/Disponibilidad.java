package com.ecommerce.vuelos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * El cupo de un vuelo para una clase concreta. Es la entidad asociativa entre
 * Vuelo y Clase, y es donde vive el stock.
 */
@Entity
@Table(name = "disponibilidades",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_disponibilidad_vuelo_clase",
                columnNames = {"vuelo_id", "clase_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vuelo_id", nullable = false)
    private Vuelo vuelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clase_id", nullable = false)
    private Clase clase;

    @Column(nullable = false)
    private Integer asientosTotales;

    @Column(nullable = false)
    private Integer asientosDisponibles;

    /** Precio de venta de esta clase. Es el que se cobra, no el precio base del vuelo. */
    @Column(nullable = false)
    private BigDecimal precio;

    /** Derivado: el precio de esta clase con el descuento del vuelo aplicado. */
    @Transient
    public BigDecimal getPrecioConDescuento() {
        BigDecimal descuento = vuelo != null && vuelo.getDescuento() != null
                ? vuelo.getDescuento()
                : BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.subtract(descuento.divide(BigDecimal.valueOf(100)));
        return precio.multiply(factor);
    }

    @Transient
    public boolean hayStock() {
        return asientosDisponibles != null && asientosDisponibles > 0;
    }
}
