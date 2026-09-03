package com.ecommerce.vuelos.dto.vuelo;

import com.ecommerce.vuelos.entity.EstadoVuelo;
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
public class VueloResponse {

    private Long id;
    private String numeroVuelo;
    private String descripcion;

    private Long categoriaId;
    private String categoriaNombre;

    private String origenIata;
    private String origenCiudad;
    private String destinoIata;
    private String destinoCiudad;

    private LocalDateTime fechaSalida;
    private LocalDateTime fechaLlegada;
    private Integer duracionMinutos;

    private BigDecimal precio;
    private BigDecimal descuento;
    private BigDecimal precioConDescuento;

    private boolean hayStock;
    private EstadoVuelo estado;

    /** Los cupos por clase, con su precio. */
    private List<DisponibilidadResponse> disponibilidades;

    private Long vendedorId;
    private String vendedorUsername;

    /**
     * Aplana el vuelo y sus relaciones. Aca se ve para que sirve el DTO: en vez
     * de mandar el Aeropuerto, la Categoria y el Usuario enteros, viajan solo los
     * campos que la pantalla necesita.
     */
    public static VueloResponse desde(Vuelo vuelo) {
        return VueloResponse.builder()
                .id(vuelo.getId())
                .numeroVuelo(vuelo.getNumeroVuelo())
                .descripcion(vuelo.getDescripcion())
                .categoriaId(vuelo.getCategoria().getId())
                .categoriaNombre(vuelo.getCategoria().getNombre())
                .origenIata(vuelo.getOrigen().getCodigoIata())
                .origenCiudad(vuelo.getOrigen().getCiudad())
                .destinoIata(vuelo.getDestino().getCodigoIata())
                .destinoCiudad(vuelo.getDestino().getCiudad())
                .fechaSalida(vuelo.getFechaSalida())
                .fechaLlegada(vuelo.getFechaLlegada())
                .duracionMinutos(vuelo.getDuracionMinutos())
                .precio(vuelo.getPrecio())
                .descuento(vuelo.getDescuento())
                .precioConDescuento(vuelo.getPrecioConDescuento())
                .hayStock(vuelo.isDisponible())
                .estado(vuelo.getEstado())
                .disponibilidades(vuelo.getDisponibilidades().stream()
                        .map(DisponibilidadResponse::desde)
                        .toList())
                .vendedorId(vuelo.getVendedor().getId())
                .vendedorUsername(vuelo.getVendedor().getUsername())
                .build();
    }
}
