package com.ecommerce.vuelos.dto.carrito;

import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.ItemCarrito;
import com.ecommerce.vuelos.entity.TipoPasajero;
import com.ecommerce.vuelos.entity.Vuelo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class ItemCarritoResponse {

    private Long id;
    private Long disponibilidadId;
    private Long vueloId;
    private String numeroVuelo;
    private String origen;
    private String destino;
    private String claseNombre;
    private TipoPasajero tipoPasajero;
    private BigDecimal precioUnitario;
    private Integer cantidad;
    private BigDecimal subtotal;


    public static ItemCarritoResponse desde(ItemCarrito item) {
        Disponibilidad disponibilidad = item.getDisponibilidad();
        Vuelo vuelo = disponibilidad.getVuelo();
        // El precio de la clase es el de un adulto; niños y bebés pagan un porcentaje
        BigDecimal precioUnitario = item.getTipoPasajero().aplicar(disponibilidad.getPrecioConDescuento());

        return ItemCarritoResponse.builder()
                .id(item.getId())
                .disponibilidadId(disponibilidad.getId())
                .vueloId(vuelo.getId())
                .numeroVuelo(vuelo.getNumeroVuelo())
                .origen(vuelo.getOrigen().getCiudad())
                .destino(vuelo.getDestino().getCiudad())
                .claseNombre(disponibilidad.getClase().getNombre())
                .tipoPasajero(item.getTipoPasajero())
                .precioUnitario(precioUnitario)
                .cantidad(item.getCantidad())
                .subtotal(precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad())))
                .build();
    }
}
