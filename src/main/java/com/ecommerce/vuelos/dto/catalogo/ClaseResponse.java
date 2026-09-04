package com.ecommerce.vuelos.dto.catalogo;

import com.ecommerce.vuelos.entity.Clase;
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

    public static ClaseResponse desde(Clase clase) {
        return ClaseResponse.builder()
                .id(clase.getId())
                .nombre(clase.getNombre())
                .descripcion(clase.getDescripcion())
                .equipajeBodega(clase.getEquipajeBodega())
                .build();
    }
}
