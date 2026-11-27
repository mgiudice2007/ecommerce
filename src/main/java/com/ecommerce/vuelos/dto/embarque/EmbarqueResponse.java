package com.ecommerce.vuelos.dto.embarque;

import com.ecommerce.vuelos.entity.EstadoEmbarque;
import com.ecommerce.vuelos.entity.ItemOrden;
import com.ecommerce.vuelos.entity.Pasaje;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Lo que ve el administrador al validar un pase de embarque. */
@Getter
@Builder
@AllArgsConstructor
public class EmbarqueResponse {

    private String codigoEmbarque;
    private EstadoEmbarque estado;
    // Por que no es valido (compra cancelada, vuelo cancelado) o cuando embarco
    private String motivo;
    private String nombre;
    private String apellido;
    private String documento;
    private Long ordenId;
    private String numeroVuelo;
    private String origen;
    private String destino;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaLlegada;
    private Integer duracionMinutos;
    private String claseNombre;
    private String asiento;
    private LocalDateTime embarcadoEn;

    public static EmbarqueResponse desde(Pasaje pasaje, EstadoEmbarque estado, String motivo) {
        ItemOrden item = pasaje.getItemOrden();
        var vuelo = item.getDisponibilidad().getVuelo();
        return EmbarqueResponse.builder()
                .codigoEmbarque(pasaje.getCodigoEmbarque())
                .estado(estado)
                .motivo(motivo)
                .nombre(pasaje.getNombre())
                .apellido(pasaje.getApellido())
                .documento(pasaje.getDocumento())
                .ordenId(item.getOrden().getId())
                .numeroVuelo(vuelo.getNumeroVuelo())
                .origen(vuelo.getOrigen().getCiudad())
                .destino(vuelo.getDestino().getCiudad())
                .fechaSalida(vuelo.getFechaSalida())
                .fechaLlegada(vuelo.getFechaLlegada())
                .duracionMinutos(vuelo.getDuracionMinutos())
                .claseNombre(item.getDisponibilidad().getClase().getNombre())
                .asiento(pasaje.getAsiento())
                .embarcadoEn(pasaje.getEmbarcadoEn())
                .build();
    }
}
