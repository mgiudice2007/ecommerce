package com.ecommerce.vuelos.dto.aerolinea;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AerolineaResponse {

    private Long id;
    private String nombre;
    private int cantidadVuelos;
}
