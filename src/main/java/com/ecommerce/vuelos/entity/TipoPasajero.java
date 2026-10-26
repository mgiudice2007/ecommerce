package com.ecommerce.vuelos.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;


public enum TipoPasajero {
    ADULTO(100),
    NINO(100),
    BEBE(0);

    private final int porcentaje;

    TipoPasajero(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    public int getPorcentaje() {
        return porcentaje;
    }

    public boolean ocupaAsiento() {
        return this != BEBE;
    }

    public BigDecimal aplicar(BigDecimal precioAdulto) {
        return precioAdulto.multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
