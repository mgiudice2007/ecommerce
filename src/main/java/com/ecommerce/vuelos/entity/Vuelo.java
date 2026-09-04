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

    /** Los descuentos cargados sobre este vuelo. A lo sumo uno esta vigente por fecha. */
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

    /**
     * El descuento que rige hoy, o null si no hay ninguno. Como al crear y al
     * modificar se valida que no haya dos vigentes solapados, aca a lo sumo hay
     * uno y no hace falta decidir cual gana.
     */
    @Transient
    public Descuento getDescuentoVigente() {
        LocalDate hoy = LocalDate.now();
        return descuentos.stream()
                .filter(d -> d.estaVigente(hoy))
                .findFirst()
                .orElse(null);
    }

    /** Derivado: se calcula, no se persiste. */
    @Transient
    public BigDecimal getPrecioConDescuento() {
        Descuento vigente = getDescuentoVigente();
        return vigente == null ? precio : vigente.aplicarA(precio);
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
