package com.ecommerce.vuelos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un descuento sobre un vuelo. Es una entidad y no un campo porque tiene dos
 * cosas que un campo no puede expresar: una vigencia (desde/hasta) y un tipo
 * (porcentaje o monto fijo). Ademas queda el historial: apagar un descuento no
 * borra el que estuvo vigente el mes pasado.
 */
@Entity
@Table(name = "descuentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Descuento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vuelo_id", nullable = false)
    private Vuelo vuelo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDescuento tipoDescuento;

    /** Si es PORCENTAJE va de 0 a 100. Si es MONTO_FIJO es plata. */
    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate fechaDesde;

    @Column(nullable = false)
    private LocalDate fechaHasta;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /** Vigente = prendido y dentro de la ventana de fechas, inclusive los bordes. */
    @Transient
    public boolean estaVigente(LocalDate fecha) {
        return Boolean.TRUE.equals(activo)
                && fechaDesde != null && fechaHasta != null
                && !fecha.isBefore(fechaDesde)
                && !fecha.isAfter(fechaHasta);
    }

    /**
     * Aplica el descuento a un precio. Nunca devuelve negativo: un monto fijo
     * mas grande que el precio deja el precio en cero, no en deuda.
     */
    @Transient
    public BigDecimal aplicarA(BigDecimal precio) {
        if (precio == null || valor == null || tipoDescuento == null) {
            return precio;
        }
        BigDecimal resultado = switch (tipoDescuento) {
            case PORCENTAJE -> precio.multiply(
                    BigDecimal.ONE.subtract(valor.divide(BigDecimal.valueOf(100))));
            case MONTO_FIJO -> precio.subtract(valor);
        };
        return resultado.max(BigDecimal.ZERO);
    }

    /**
     * Dos descuentos se solapan si comparten al menos un dia. Se usa para no
     * dejar cargar dos vigentes a la vez sobre el mismo vuelo: si se pudiera,
     * no habria forma de decidir cual gana.
     */
    @Transient
    public boolean seSolapaCon(LocalDate desde, LocalDate hasta) {
        return !fechaDesde.isAfter(hasta) && !fechaHasta.isBefore(desde);
    }
}
