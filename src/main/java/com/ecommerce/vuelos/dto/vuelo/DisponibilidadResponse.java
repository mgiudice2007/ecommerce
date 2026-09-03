package com.ecommerce.vuelos.dto.vuelo;

import com.ecommerce.vuelos.entity.Disponibilidad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class DisponibilidadResponse {

    private Long id;
    private Long vueloId;
    private Long claseId;
    private String claseNombre;
    private Integer asientosTotales;
    private Integer asientosDisponibles;
    private BigDecimal precio;
    private BigDecimal precioConDescuento;
    private boolean hayStock;

    public static DisponibilidadResponse desde(Disponibilidad disponibilidad) {
        return DisponibilidadResponse.builder()
                .id(disponibilidad.getId())
                .vueloId(disponibilidad.getVuelo().getId())
                .claseId(disponibilidad.getClase().getId())
                .claseNombre(disponibilidad.getClase().getNombre())
                .asientosTotales(disponibilidad.getAsientosTotales())
                .asientosDisponibles(disponibilidad.getAsientosDisponibles())
                .precio(disponibilidad.getPrecio())
                .precioConDescuento(disponibilidad.getPrecioConDescuento())
                .hayStock(disponibilidad.hayStock())
                .build();
    }
}
