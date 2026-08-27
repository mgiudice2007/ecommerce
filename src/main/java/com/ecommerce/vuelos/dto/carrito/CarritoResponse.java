package com.ecommerce.vuelos.dto.carrito;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CarritoResponse {

    private Long id;
    private List<ItemCarritoResponse> items;
    private BigDecimal total;
}
