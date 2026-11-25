package com.ecommerce.vuelos.service;

public interface QrService {

    /** Dibuja el QR del texto como una imagen PNG cuadrada de `lado` pixeles. */
    byte[] png(String texto, int lado);
}
