package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.reserva.ItemReservaResponse;
import com.ecommerce.vuelos.dto.reserva.ReservaResponse;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.model.EstadoReserva;
import com.ecommerce.vuelos.model.ItemReserva;
import com.ecommerce.vuelos.model.Reserva;
import com.ecommerce.vuelos.model.Vuelo;
import com.ecommerce.vuelos.repository.ReservaRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final VueloRepository vueloRepository;

    public List<ReservaResponse> historial(Long pasajeroId) {
        return reservaRepository.findByPasajeroIdOrderByFechaDesc(pasajeroId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ReservaResponse obtener(Long pasajeroId, Long reservaId) {
        return toResponse(buscarReservaDelPasajero(pasajeroId, reservaId));
    }

    @Transactional
    public ReservaResponse cancelar(Long pasajeroId, Long reservaId) {
        Reserva reserva = buscarReservaDelPasajero(pasajeroId, reservaId);

        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new BadRequestException("La reserva ya se encuentra cancelada");
        }

        for (ItemReserva item : reserva.getItems()) {
            Vuelo vuelo = item.getVuelo();
            vuelo.setAsientosDisponibles(vuelo.getAsientosDisponibles() + item.getCantidad());
            vueloRepository.save(vuelo);
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        Reserva guardada = reservaRepository.save(reserva);
        return toResponse(guardada);
    }

    private Reserva buscarReservaDelPasajero(Long pasajeroId, Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + reservaId));

        if (!reserva.getPasajero().getId().equals(pasajeroId)) {
            throw new ResourceNotFoundException("Reserva no encontrada: " + reservaId);
        }

        return reserva;
    }

    public ReservaResponse toResponse(Reserva reserva) {
        List<ItemReservaResponse> items = reserva.getItems().stream()
                .map(item -> ItemReservaResponse.builder()
                        .id(item.getId())
                        .vueloId(item.getVuelo().getId())
                        .origen(item.getVuelo().getOrigen())
                        .destino(item.getVuelo().getDestino())
                        .cantidad(item.getCantidad())
                        .precioUnitario(item.getPrecioUnitario())
                        .subtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                        .build())
                .toList();

        return ReservaResponse.builder()
                .id(reserva.getId())
                .total(reserva.getTotal())
                .fecha(reserva.getFecha())
                .estado(reserva.getEstado())
                .items(items)
                .build();
    }
}
