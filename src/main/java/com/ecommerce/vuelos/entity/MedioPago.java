package com.ecommerce.vuelos.entity;

public enum MedioPago {
    TARJETA_CREDITO,
    TARJETA_DEBITO,
    TRANSFERENCIA,
    BILLETERA_VIRTUAL,
    MILLAS;

    public boolean esTarjeta() {
        return this == TARJETA_CREDITO || this == TARJETA_DEBITO;
    }
}
