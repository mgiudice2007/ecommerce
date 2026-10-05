package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    // Texto comun y no el ENUM de MySQL: ddl-auto=update no agrega valores a un ENUM ya creado,
    // asi que sumar un estado (como DEMORADO) fallaria con "Data truncated" en una base existente.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoVuelo estado = EstadoVuelo.ACTIVO;

    @Column(nullable = false)
    private LocalDateTime fechaAlta;

    @Column
    private LocalDateTime fechaBaja;


    @Transient
    public Descuento getDescuentoVigente() {
        LocalDate hoy = LocalDate.now();
        return descuentos.stream()
                .filter(d -> d.estaVigente(hoy))
                .findFirst()
                .orElse(null);
    }

    @Transient
    public BigDecimal getPrecioConDescuento() {
        Descuento vigente = getDescuentoVigente();
        return vigente == null ? precio : vigente.aplicarA(precio);
    }

    @Transient
    public Integer getDuracionMinutos() {
        if (fechaSalida == null || fechaLlegada == null) {
            return null;
        }
        return (int) Duration.between(fechaSalida, fechaLlegada).toMinutes();
    }

    /** Un vuelo demorado sigue operando: se lista y se puede comprar. */
    @Transient
    public boolean estaOperativo() {
        return estado == EstadoVuelo.ACTIVO || estado == EstadoVuelo.DEMORADO;
    }

    /** Hay stock si el vuelo esta operativo y al menos una clase tiene asientos. */
    @Transient
    public boolean isDisponible() {
        return estaOperativo()
                && disponibilidades.stream().anyMatch(Disponibilidad::hayStock);
    }
}
