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

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal descuentoTotal = BigDecimal.ZERO;

    @Column
    private Integer millasGanadas;

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

    public LocalDateTime primeraSalida() {
        LocalDateTime primera = null;
        for (ItemOrden item : items) {
            LocalDateTime salida = item.getDisponibilidad().getVuelo().getFechaSalida();
            if (primera == null || salida.isBefore(primera)) {
                primera = salida;
            }
        }
        return primera;
    }
}
