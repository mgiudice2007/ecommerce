package com.ecommerce.vuelos.dto.vuelo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Los campos que acompanan al archivo. No lleva @RequestBody: en una request
 * multipart los campos vienen como partes del formulario, no como JSON, y por
 * eso el controller lo recibe con @ModelAttribute.
 */
@Getter
@Setter
@NoArgsConstructor
public class FotoRequest {

    private Long vueloId;

    /** Opcional. Si no viene, la foto se agrega al final. */
    private Integer orden;
}
