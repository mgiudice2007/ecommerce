package com.ecommerce.vuelos.dto.vuelo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ClaseResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean equipajeBodega;
}
