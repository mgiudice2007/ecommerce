package com.ecommerce.vuelos.dto.orden;

import com.ecommerce.vuelos.entity.AsientoOcupado;
import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.ItemOrden;
import com.ecommerce.vuelos.entity.TipoPasajero;
import com.ecommerce.vuelos.entity.Vuelo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    // Codigo y nombre de los aeropuertos, para el itinerario del PDF
    private String origenIata;
    private String destinoIata;
    private String origenAeropuerto;
    private String destinoAeropuerto;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaLlegada;
    private Integer duracionMinutos;
    private String claseNombre;
    private TipoPasajero tipoPasajero;
    private Integer cantidad;
    private List<String> asientos;
    private List<PasajeResponse> pasajes;
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
                .origenIata(vuelo.getOrigen().getCodigoIata())
                .destinoIata(vuelo.getDestino().getCodigoIata())
                .origenAeropuerto(vuelo.getOrigen().getNombre())
                .destinoAeropuerto(vuelo.getDestino().getNombre())
                .fechaSalida(vuelo.getFechaSalida())
                .fechaLlegada(vuelo.getFechaLlegada())
                .duracionMinutos(vuelo.getDuracionMinutos())
                .claseNombre(disponibilidad.getClase().getNombre())
                .tipoPasajero(item.getTipoPasajero())
                .cantidad(item.getCantidad())
                .asientos(item.getAsientos().stream().map(AsientoOcupado::getCodigo).toList())
                .pasajes(item.getPasajes().stream().map(PasajeResponse::desde).toList())
                .precioUnitario(item.getPrecioUnitario())
                .descuentoAplicado(item.getDescuentoAplicado())
                .subtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                .build();
    }
}
