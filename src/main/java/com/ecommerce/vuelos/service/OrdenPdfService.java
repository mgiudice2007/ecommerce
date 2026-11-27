package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.OrdenResponse;

public interface OrdenPdfService {

    /** Arma la orden de compra en PDF: datos del cliente, vuelos, totales y pasajeros con su codigo. */
    byte[] ordenDeCompra(OrdenResponse orden, String nombreCliente, String emailCliente);
}
