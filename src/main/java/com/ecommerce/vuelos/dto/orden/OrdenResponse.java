package com.ecommerce.vuelos.dto.orden;

import com.ecommerce.vuelos.entity.EstadoOrden;
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
}
