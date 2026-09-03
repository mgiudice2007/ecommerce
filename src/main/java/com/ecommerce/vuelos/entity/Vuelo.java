package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "vuelos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vuelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** El vendedor que publico este vuelo. Es su dueño. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen_iata", nullable = false)
    private Aeropuerto origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino_iata", nullable = false)
    private Aeropuerto destino;

    @Column(nullable = false)
    private String numeroVuelo;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaSalida;

    @Column(nullable = false)
    private LocalDateTime fechaLlegada;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer asientosDisponibles;

    /** Porcentaje de descuento, de 0 a 100. Pasa a la entidad Descuento en la fase 4. */
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaseVuelo clase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoVuelo estado = EstadoVuelo.ACTIVO;

    @Column(nullable = false)
    private LocalDateTime fechaAlta;

    @Column
    private LocalDateTime fechaBaja;

    /** Derivado: se calcula, no se persiste. */
    @Transient
    public BigDecimal getPrecioConDescuento() {
        BigDecimal factor = BigDecimal.ONE.subtract(descuento.divide(BigDecimal.valueOf(100)));
        return precio.multiply(factor);
    }

    /** Derivado: la diferencia entre salida y llegada. */
    @Transient
    public Integer getDuracionMinutos() {
        if (fechaSalida == null || fechaLlegada == null) {
            return null;
        }
        return (int) Duration.between(fechaSalida, fechaLlegada).toMinutes();
    }

    @Transient
    public boolean isDisponible() {
        return estado == EstadoVuelo.ACTIVO && asientosDisponibles != null && asientosDisponibles > 0;
    }
}
