package com.ecommerce.vuelos.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tipo de pasajero segun la edad, como en las aerolineas: el niño paga la misma
 * tarifa que un adulto y el bebé viaja gratis, a upa de un adulto.
 * NINO y BEBE van sin ñ ni tilde porque son nombres de codigo.
 */
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

    /** Los bebés viajan a upa de un adulto: no eligen asiento ni descuentan un lugar del cupo. */
    public boolean ocupaAsiento() {
        return this != BEBE;
    }

    /** Lo que paga este pasajero sobre el precio de un adulto. */
    public BigDecimal aplicar(BigDecimal precioAdulto) {
        return precioAdulto.multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
