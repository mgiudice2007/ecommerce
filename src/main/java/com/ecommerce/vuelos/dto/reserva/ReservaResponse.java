package com.ecommerce.vuelos.dto.reserva;

import com.ecommerce.vuelos.model.EstadoReserva;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ReservaResponse {

    private Long id;
    private BigDecimal total;
    private LocalDateTime fecha;
    private EstadoReserva estado;
    private List<ItemReservaResponse> items;
}
