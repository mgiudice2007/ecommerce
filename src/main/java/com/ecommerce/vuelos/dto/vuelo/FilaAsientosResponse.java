package com.ecommerce.vuelos.dto.vuelo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Una fila del avion: su numero y los codigos de sus asientos ("12A", "12B"...). */
@Getter
@AllArgsConstructor
public class FilaAsientosResponse {

    private Integer numero;
    private List<String> asientos;
}
