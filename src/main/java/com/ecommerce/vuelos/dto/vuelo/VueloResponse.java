package com.ecommerce.vuelos.dto.vuelo;

import com.ecommerce.vuelos.entity.ClaseVuelo;
import com.ecommerce.vuelos.entity.EstadoVuelo;
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

    private Integer asientosDisponibles;
    private boolean hayStock;

    private ClaseVuelo clase;
    private EstadoVuelo estado;

    private Long vendedorId;
    private String vendedorUsername;
}
