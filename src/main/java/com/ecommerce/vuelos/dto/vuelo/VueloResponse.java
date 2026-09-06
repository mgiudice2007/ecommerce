package com.ecommerce.vuelos.dto.vuelo;

import com.ecommerce.vuelos.dto.descuento.DescuentoResponse;
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

    private DescuentoResponse descuentoVigente;
    private BigDecimal precioConDescuento;

    private boolean hayStock;
    private EstadoVuelo estado;

    private List<DisponibilidadResponse> disponibilidades;

    private Long vendedorId;
    private String vendedorUsername;

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
                .descuentoVigente(vuelo.getDescuentoVigente() == null
                        ? null
                        : DescuentoResponse.desde(vuelo.getDescuentoVigente()))
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
