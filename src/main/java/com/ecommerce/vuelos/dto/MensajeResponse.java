package com.ecommerce.vuelos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Respuesta simple para las operaciones que no devuelven un recurso (delete, logout). */
@Getter
@AllArgsConstructor
public class MensajeResponse {
    private String mensaje;
}
