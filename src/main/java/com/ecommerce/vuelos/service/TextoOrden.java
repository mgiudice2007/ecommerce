package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.entity.TipoPasajero;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Textos de una orden que comparten el mail y el PDF, para que digan lo mismo. */
final class TextoOrden {

    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");

    private TextoOrden() {
    }

    static String plata(BigDecimal monto) {
        if (monto == null || monto.signum() == 0) {
            return "Gratis";
        }
        return NumberFormat.getCurrencyInstance(ARGENTINA).format(monto);
    }

    static String tipoPasajero(TipoPasajero tipo) {
        return switch (tipo) {
            case ADULTO -> "adulto";
            case NINO -> "niño";
            case BEBE -> "bebé";
        };
    }

    static String medioPago(OrdenResponse orden) {
        if (orden.getMedioPago() == null) {
            return "-";
        }
        String nombre = switch (orden.getMedioPago()) {
            case TARJETA_CREDITO -> "Tarjeta de crédito";
            case TARJETA_DEBITO -> "Tarjeta de débito";
            case TRANSFERENCIA -> "Transferencia";
            case BILLETERA_VIRTUAL -> "Billetera virtual";
            case MILLAS -> "Millas";
        };
        return orden.getUltimosDigitos() == null ? nombre : nombre + " terminada en " + orden.getUltimosDigitos();
    }
}
