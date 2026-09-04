package com.ecommerce.vuelos.dto.catalogo;

import com.ecommerce.vuelos.entity.Categoria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CategoriaResponse {

    private Long id;
    private String nombre;
    private String descripcion;

    public static CategoriaResponse desde(Categoria categoria) {
        return CategoriaResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .build();
    }
}
