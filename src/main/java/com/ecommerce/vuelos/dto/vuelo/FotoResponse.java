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

    /** Tamano en bytes, para que el cliente sepa que va a descargar. */
    private Integer tamano;

    /**
     * A proposito no expone el campo datos. Si el binario viajara aca, un
     * listado de vuelos con fotos pesaria megabytes; el que quiere la imagen
     * la pide por GET /api/fotos/{id}.
     */
    public static FotoResponse desde(Foto foto) {
        return FotoResponse.builder()
                .id(foto.getId())
                .vueloId(foto.getVuelo().getId())
                .nombreArchivo(foto.getNombreArchivo())
                .orden(foto.getOrden())
                .tamano(foto.getDatos() != null ? foto.getDatos().length : 0)
                .build();
    }
}
