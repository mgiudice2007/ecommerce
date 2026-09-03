package com.ecommerce.vuelos.dto.orden;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class ItemOrdenResponse {

    private Long id;
    private Long vueloId;
    private String origen;
    private String destino;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
