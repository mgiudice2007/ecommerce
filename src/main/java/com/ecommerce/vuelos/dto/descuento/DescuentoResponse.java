package com.ecommerce.vuelos.dto.descuento;

import com.ecommerce.vuelos.entity.Descuento;
import com.ecommerce.vuelos.entity.TipoDescuento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class DescuentoResponse {

    private Long id;
    private Long vueloId;
    private TipoDescuento tipoDescuento;
    private BigDecimal valor;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private Boolean activo;

    /** Calculado al momento de responder: si hoy cae dentro de la ventana. */
    private boolean vigente;

    public static DescuentoResponse desde(Descuento descuento) {
        return DescuentoResponse.builder()
                .id(descuento.getId())
                .vueloId(descuento.getVuelo().getId())
                .tipoDescuento(descuento.getTipoDescuento())
                .valor(descuento.getValor())
                .fechaDesde(descuento.getFechaDesde())
                .fechaHasta(descuento.getFechaHasta())
                .activo(descuento.getActivo())
                .vigente(descuento.estaVigente(LocalDate.now()))
                .build();
    }
}
