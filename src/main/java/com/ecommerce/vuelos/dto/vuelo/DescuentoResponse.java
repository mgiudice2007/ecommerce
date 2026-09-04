package com.ecommerce.vuelos.dto.vuelo;

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
    private boolean activo;
    private boolean vigente;

    public static DescuentoResponse desde(Descuento descuento) {
        LocalDate hoy = LocalDate.now();
        boolean vigente = Boolean.TRUE.equals(descuento.getActivo())
                && !hoy.isBefore(descuento.getFechaDesde())
                && !hoy.isAfter(descuento.getFechaHasta());

        return DescuentoResponse.builder()
                .id(descuento.getId())
                .vueloId(descuento.getVuelo().getId())
                .tipoDescuento(descuento.getTipoDescuento())
                .valor(descuento.getValor())
                .fechaDesde(descuento.getFechaDesde())
                .fechaHasta(descuento.getFechaHasta())
                .activo(Boolean.TRUE.equals(descuento.getActivo()))
                .vigente(vigente)
                .build();
    }
}
