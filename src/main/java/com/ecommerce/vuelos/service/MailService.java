package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.event.OrdenConfirmadaEvent;
import com.ecommerce.vuelos.event.PasajeroEmbarcadoEvent;

public interface MailService {

    /**
     * Manda al comprador el detalle de su compra. Corre despues del commit del checkout y en otro
     * hilo: si el mail falla (SMTP caido, credenciales mal), la compra queda confirmada igual.
     */
    void enviarConfirmacionCompra(OrdenConfirmadaEvent evento);

    /**
     * Le avisa al comprador que se valido el pase de embarque de un pasajero y le desea buen viaje.
     * Igual que la confirmacion: despues del commit y en otro hilo; si falla, el embarque queda hecho.
     */
    void enviarBuenViaje(PasajeroEmbarcadoEvent evento);
}
