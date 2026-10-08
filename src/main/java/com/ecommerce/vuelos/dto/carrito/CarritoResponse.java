package com.ecommerce.vuelos.dto.carrito;

import com.ecommerce.vuelos.entity.Carrito;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class CarritoResponse {

    private List<ItemCarritoResponse> items;
    private BigDecimal total;

    /** Millas que suma esta compra si se paga toda con plata. */
    private Integer millasAGanar;
    /** Saldo de millas del comprador y cuantos pesos vale cada una al usarlas. */
    private Integer millasDisponibles;
    private BigDecimal valorMilla;

    public static CarritoResponse desde(Carrito carrito) {
        List<ItemCarritoResponse> items = carrito.getItems().stream()
                .map(ItemCarritoResponse::desde)
                .toList();

        BigDecimal total = items.stream()
                .map(ItemCarritoResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CarritoResponse.builder()
                .items(items)
                .total(total)
                .build();
    }
}
