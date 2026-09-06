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

    /** Tiene 3 métodos que no se guardan en la base: se calculan al vuelo, no son una columna. */
    /** "si el vuelo tiene un descuento vigente ahora mismo se lo aplica al precio. Si no tiene, el precio es el normal" */
    @Transient
    public BigDecimal getPrecioConDescuento() {
        Descuento vigente = vuelo != null ? vuelo.getDescuentoVigente() : null;
        return vigente == null ? precio : vigente.aplicarA(precio);
    }

    /** precio original menos precio con descuento = cuánto te ahorrás por asiento. */
    @Transient
    public BigDecimal getDescuentoUnitario() {
        return precio.subtract(getPrecioConDescuento());
    }

   /** ¿queda algo para vender? */
    @Transient
    public boolean hayStock() {
        return asientosDisponibles != null && asientosDisponibles > 0;
    }
}
