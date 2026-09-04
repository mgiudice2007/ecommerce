package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "vuelo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Disponibilidad> disponibilidades = new ArrayList<>();

    /** Promociones de este vuelo. El precio usa la que este vigente hoy, si hay alguna. */
    @OneToMany(mappedBy = "vuelo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Descuento> descuentos = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoVuelo estado = EstadoVuelo.ACTIVO;

    @Column(nullable = false)
    private LocalDateTime fechaAlta;

    @Column
    private LocalDateTime fechaBaja;

    /** El descuento activo cuya vigencia cubre hoy, si hay alguno. */
    @Transient
    public Descuento getDescuentoVigente() {
        LocalDate hoy = LocalDate.now();
        return descuentos.stream()
                .filter(d -> Boolean.TRUE.equals(d.getActivo()))
                .filter(d -> !hoy.isBefore(d.getFechaDesde()) && !hoy.isAfter(d.getFechaHasta()))
                .findFirst()
                .orElse(null);
    }

    /** Aplica el descuento vigente (si hay) a un precio base. Nunca da negativo. */
    @Transient
    public BigDecimal aplicarDescuento(BigDecimal precioBase) {
        Descuento vigente = getDescuentoVigente();
        if (vigente == null) {
            return precioBase;
        }
        BigDecimal resultado = vigente.getTipoDescuento() == TipoDescuento.PORCENTAJE
                ? precioBase.multiply(BigDecimal.ONE.subtract(vigente.getValor().divide(BigDecimal.valueOf(100))))
                : precioBase.subtract(vigente.getValor());
        return resultado.max(BigDecimal.ZERO);
    }

    /** Derivado: se calcula, no se persiste. */
    @Transient
    public BigDecimal getPrecioConDescuento() {
        return aplicarDescuento(precio);
    }

    /** Derivado: la diferencia entre salida y llegada. */
    @Transient
    public Integer getDuracionMinutos() {
        if (fechaSalida == null || fechaLlegada == null) {
            return null;
        }
        return (int) Duration.between(fechaSalida, fechaLlegada).toMinutes();
    }

    /** Hay stock si el vuelo esta activo y al menos una clase tiene asientos. */
    @Transient
    public boolean isDisponible() {
        return estado == EstadoVuelo.ACTIVO
                && disponibilidades.stream().anyMatch(Disponibilidad::hayStock);
    }
}
