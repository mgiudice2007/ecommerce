package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.event.OrdenConfirmadaEvent;

public interface MailService {

    /**
     * Manda al comprador el detalle de su compra. Corre despues del commit del checkout y en otro
     * hilo: si el mail falla (SMTP caido, credenciales mal), la compra queda confirmada igual.
     */
    void enviarConfirmacionCompra(OrdenConfirmadaEvent evento);
}
