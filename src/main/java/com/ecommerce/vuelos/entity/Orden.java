package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Lo que se cobro, ya con los descuentos restados. */
    @Column(nullable = false)
    private BigDecimal total;

    /** Cuanto se ahorro el comprador en esta orden. Queda congelado como el total. */
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal descuentoTotal = BigDecimal.ZERO;

    /** Millas que sumo el comprador con esta orden. */
    @Column
    private Integer millasGanadas;

    /** Millas que uso para pagar y cuantos pesos le descontaron por ellas. */
    @Column
    private Integer millasUsadas;

    @Column
    private BigDecimal descuentoMillas;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemOrden> items = new ArrayList<>();

    // Las ordenes viejas (de antes de las millas) no tienen estos datos
    public Integer getMillasGanadas() {
        return millasGanadas == null ? 0 : millasGanadas;
    }

    public Integer getMillasUsadas() {
        return millasUsadas == null ? 0 : millasUsadas;
    }

    public BigDecimal getDescuentoMillas() {
        return descuentoMillas == null ? BigDecimal.ZERO : descuentoMillas;
    }
}
