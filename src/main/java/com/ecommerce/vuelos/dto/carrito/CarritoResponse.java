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

    private List<ItemCarritoResponse> items;
    private BigDecimal total;

 
    private Integer millasAGanar;
    
    private Integer millasDisponibles;
    private BigDecimal valorMilla;

    public static CarritoResponse desde(Carrito carrito, Integer millasAGanar, Integer millasDisponibles,
                                        BigDecimal valorMilla) {
        List<ItemCarritoResponse> items = carrito.getItems().stream()
                .map(ItemCarritoResponse::desde)
                .toList();

        BigDecimal total = items.stream()
                .map(ItemCarritoResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CarritoResponse.builder()
                .items(items)
                .total(total)
                .millasAGanar(millasAGanar)
                .millasDisponibles(millasDisponibles)
                .valorMilla(valorMilla)
                .build();
    }
}
