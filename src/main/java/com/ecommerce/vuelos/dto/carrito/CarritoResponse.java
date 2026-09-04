package com.ecommerce.vuelos.dto.carrito;

import com.ecommerce.vuelos.entity.Carrito;
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

    public static CarritoResponse desde(Carrito carrito) {
        List<ItemCarritoResponse> items = carrito.getItems().stream()
                .map(ItemCarritoResponse::desde)
                .toList();

        BigDecimal total = items.stream()
                .map(ItemCarritoResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CarritoResponse.builder()
                .id(carrito.getId())
                .items(items)
                .total(total)
                .build();
    }
}
