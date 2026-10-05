package com.ecommerce.vuelos.dto.vuelo;

import com.ecommerce.vuelos.entity.EstadoVuelo;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** El backend no se queda en silencio: devuelve el estado resultante y un mensaje para mostrar. */
@Getter
@AllArgsConstructor
public class EstadoVueloResponse {
    private Long id;
    private EstadoVuelo estado;
    private String mensaje;
}
