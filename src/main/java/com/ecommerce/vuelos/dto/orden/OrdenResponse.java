package com.ecommerce.vuelos.dto.orden;

import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.Orden;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class OrdenResponse {

    private Long id;
    private BigDecimal total;

    private BigDecimal descuentoTotal;
    private Integer millasGanadas;
    private Integer millasUsadas;
    private BigDecimal descuentoMillas;
    private LocalDateTime fecha;
    private EstadoOrden estado;
    private List<ItemOrdenResponse> items;

    /** Hasta cuando se puede cancelar (horas antes de que salga el primer vuelo). */
    private LocalDateTime cancelableHasta;
    /** true si la compra no esta cancelada y todavia no paso cancelableHasta. */
    private boolean sePuedeCancelar;

    /** horasParaCancelar: viene de application.properties (ordenes.horas-para-cancelar). */
    public static OrdenResponse desde(Orden orden, int horasParaCancelar) {
        LocalDateTime cancelableHasta = orden.primeraSalida() == null
                ? null
                : orden.primeraSalida().minusHours(horasParaCancelar);
        boolean sePuedeCancelar = orden.getEstado() != EstadoOrden.CANCELADA
                && cancelableHasta != null
                && LocalDateTime.now().isBefore(cancelableHasta);

        return OrdenResponse.builder()
                .id(orden.getId())
                .total(orden.getTotal())
                .descuentoTotal(orden.getDescuentoTotal())
                .millasGanadas(orden.getMillasGanadas())
                .millasUsadas(orden.getMillasUsadas())
                .descuentoMillas(orden.getDescuentoMillas())
                .fecha(orden.getFecha())
                .estado(orden.getEstado())
                .items(orden.getItems().stream()
                        .map(ItemOrdenResponse::desde)
                        .toList())
                .cancelableHasta(cancelableHasta)
                .sePuedeCancelar(sePuedeCancelar)
                .build();
    }
}
