package com.ecommerce.vuelos.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Se publica cuando un vuelo con pasajes vendidos se demora, se cancela, vuelve a operar o cambia de
 * horario. Lleva los datos del vuelo y los compradores ya copiados, por la misma razon que
 * {@link OrdenConfirmadaEvent}: el mail sale despues del commit y en otro hilo. Los horarios
 * anteriores solo vienen cuando el vuelo se reprogramo.
 */
public record VueloModificadoEvent(Motivo motivo, String numeroVuelo, String origen, String destino,
                                   LocalDateTime salida, LocalDateTime llegada,
                                   LocalDateTime salidaAnterior, LocalDateTime llegadaAnterior,
                                   List<Comprador> compradores) {

    public enum Motivo {
        DEMORADO,
        NORMALIZADO,
        CANCELADO,
        REPROGRAMADO
    }

    /** A quien le llega el aviso: el que compro, que puede no ser el que viaja. */
    public record Comprador(String mail, String nombre) {
    }
}
