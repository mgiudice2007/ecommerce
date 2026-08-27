package com.ecommerce.vuelos.dto.carrito;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class ItemCarritoResponse {

    private Long id;
    private Long vueloId;
    private String origen;
    private String destino;
    private BigDecimal precioUnitario;
    private Integer cantidad;
    private BigDecimal subtotal;
}
