package com.ecommerce.vuelos.event;

import com.ecommerce.vuelos.dto.orden.OrdenResponse;

/**
 * Se publica cuando el checkout termina de armar la orden. Lleva todo lo que el mail necesita ya
 * copiado en un DTO, porque el mail sale despues del commit y en otro hilo, cuando la sesion de
 * Hibernate ya se cerro y las relaciones lazy de la orden no se pueden leer.
 */
public record OrdenConfirmadaEvent(String destinatario, String nombre, OrdenResponse orden) {
}
