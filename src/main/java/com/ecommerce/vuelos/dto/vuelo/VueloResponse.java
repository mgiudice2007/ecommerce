package com.ecommerce.vuelos.dto.vuelo;

import com.ecommerce.vuelos.entity.ClaseVuelo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class VueloResponse {

    private Long id;
    private String origen;
    private String destino;
    private LocalDateTime fechaSalida;
    private BigDecimal precio;
    private BigDecimal descuento;
    private BigDecimal precioConDescuento;
    private Integer asientosDisponibles;
    private ClaseVuelo clase;
    private Long aerolineaId;
    private String aerolineaNombre;
}
