package com.ecommerce.vuelos.dto.vuelo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** El mapa de asientos de un vuelo y clase: sus filas y cuales ya estan vendidos. */
@Getter
@Builder
@AllArgsConstructor
public class MapaAsientosResponse {

    private Long disponibilidadId;
    private String claseNombre;
    /** Las letras de cada fila, por ejemplo A B C D E F en Economica. */
    private List<String> letras;
    private List<FilaAsientosResponse> filas;
    private List<String> ocupados;
}
