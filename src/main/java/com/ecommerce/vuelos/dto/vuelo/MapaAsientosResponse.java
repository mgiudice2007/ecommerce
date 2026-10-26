package com.ecommerce.vuelos.dto.vuelo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MapaAsientosResponse {

    private Long disponibilidadId;
    private String claseNombre;
    private List<String> letras;
    private List<FilaAsientosResponse> filas;
    private List<String> ocupados;
}
