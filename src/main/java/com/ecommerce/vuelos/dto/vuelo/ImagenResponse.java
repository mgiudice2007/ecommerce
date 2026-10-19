package com.ecommerce.vuelos.dto.vuelo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * La imagen de una foto, como en el ejemplo de clase de subida de imagenes:
 * el archivo viaja como texto en Base64 dentro del JSON. El frontend la muestra
 * con src="data:{tipo};base64,{file}".
 */
@Getter
@Builder
@AllArgsConstructor
public class ImagenResponse {

    private Long id;
    private String file;
    /** Tipo de imagen (image/jpeg, image/png...) para armar el data URL. */
    private String tipo;
}
