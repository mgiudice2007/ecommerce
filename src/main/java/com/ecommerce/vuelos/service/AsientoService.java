package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.MapaAsientosResponse;
import com.ecommerce.vuelos.entity.Disponibilidad;

import java.util.List;
import java.util.Set;

public interface AsientoService {

    MapaAsientosResponse mapa(Long disponibilidadId);

    /** Todos los asientos de un vuelo y clase, en orden ("11A", "11B"...). */
    List<String> codigos(Disponibilidad disponibilidad);

    /** Los asientos ya vendidos de un vuelo y clase. */
    Set<String> ocupados(Long disponibilidadId);
}
