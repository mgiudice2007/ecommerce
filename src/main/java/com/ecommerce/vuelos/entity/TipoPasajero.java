package com.ecommerce.vuelos.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tipo de pasajero segun la edad, como en las aerolineas: el nino paga el 75%
 * de la tarifa y el bebe (viaja a upa de un adulto) el 10%.
 */
public enum TipoPasajero {
    ADULTO(100),
    NINO(75),
    BEBE(10);

    private final int porcentaje;

    TipoPasajero(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    public int getPorcentaje() {
        return porcentaje;
    }

    /** Lo que paga este pasajero sobre el precio de un adulto. */
    public BigDecimal aplicar(BigDecimal precioAdulto) {
        return precioAdulto.multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
