package com.ecommerce.vuelos.event;

import com.ecommerce.vuelos.dto.embarque.EmbarqueResponse;

/**
 * Se publica cuando el administrador valida un pase y el pasajero embarca. Va al comprador de la
 * orden (que puede no ser el que viaja) con los datos del pase ya copiados en el DTO, por la misma
 * razon que {@link OrdenConfirmadaEvent}: el mail sale despues del commit y en otro hilo.
 */
public record PasajeroEmbarcadoEvent(String destinatario, String nombreComprador, EmbarqueResponse embarque) {
}
