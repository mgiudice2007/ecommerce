package com.ecommerce.vuelos.dto.orden;

import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.ItemOrden;
import com.ecommerce.vuelos.entity.Vuelo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class ItemOrdenResponse {

    private Long id;
    private Long disponibilidadId;
    private Long vueloId;
    private String numeroVuelo;
    private String origen;
    private String destino;
    private String claseNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;

    private BigDecimal descuentoAplicado;
    private BigDecimal subtotal;

    public static ItemOrdenResponse desde(ItemOrden item) {
        Disponibilidad disponibilidad = item.getDisponibilidad();
        Vuelo vuelo = disponibilidad.getVuelo();

        return ItemOrdenResponse.builder()
                .id(item.getId())
                .disponibilidadId(disponibilidad.getId())
                .vueloId(vuelo.getId())
                .numeroVuelo(vuelo.getNumeroVuelo())
                .origen(vuelo.getOrigen().getCiudad())
                .destino(vuelo.getDestino().getCiudad())
                .claseNombre(disponibilidad.getClase().getNombre())
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .descuentoAplicado(item.getDescuentoAplicado())
                .subtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                .build();
    }
}
