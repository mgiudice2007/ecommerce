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
    private LocalDateTime fecha;
    private EstadoOrden estado;
    private List<ItemOrdenResponse> items;

    public static OrdenResponse desde(Orden orden) {
        return OrdenResponse.builder()
                .id(orden.getId())
                .total(orden.getTotal())
                .fecha(orden.getFecha())
                .estado(orden.getEstado())
                .items(orden.getItems().stream()
                        .map(ItemOrdenResponse::desde)
                        .toList())
                .build();
    }
}
