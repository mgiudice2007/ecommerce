package com.ecommerce.vuelos.dto.vuelo;

import com.ecommerce.vuelos.entity.EstadoVuelo;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class EstadoVueloResponse {
    private Long id;
    private EstadoVuelo estado;
    private String mensaje;
}
