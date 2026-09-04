package com.ecommerce.vuelos.dto.vuelo;

import com.ecommerce.vuelos.entity.Foto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FotoResponse {

    private Long id;
    private Long vueloId;
    private String nombreArchivo;
    private Integer orden;
    private long tamanioBytes;

    public static FotoResponse desde(Foto foto) {
        return FotoResponse.builder()
                .id(foto.getId())
                .vueloId(foto.getVuelo().getId())
                .nombreArchivo(foto.getNombreArchivo())
                .orden(foto.getOrden())
                .tamanioBytes(foto.getDatos() != null ? foto.getDatos().length : 0)
                .build();
    }
}
