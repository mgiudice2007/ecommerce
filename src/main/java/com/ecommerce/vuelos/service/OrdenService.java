package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.ItemOrdenResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.ItemOrden;
import com.ecommerce.vuelos.entity.Orden;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.repository.OrdenRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdenService {

    private final OrdenRepository ordenRepository;
    private final VueloRepository vueloRepository;

    public List<OrdenResponse> historial(Long usuarioId) {
        return ordenRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    public OrdenResponse obtener(Long usuarioId, Long ordenId) {
        return toResponse(buscarOrdenDelPasajero(usuarioId, ordenId));
    }

    @Transactional
    public OrdenResponse cancelar(Long usuarioId, Long ordenId) {
        Orden orden = buscarOrdenDelPasajero(usuarioId, ordenId);

        if (orden.getEstado() == EstadoOrden.CANCELADA) {
            throw new BadRequestException("La orden ya se encuentra cancelada");
        }

        for (ItemOrden item : orden.getItems()) {
            Vuelo vuelo = item.getVuelo();
            vuelo.setAsientosDisponibles(vuelo.getAsientosDisponibles() + item.getCantidad());
            vueloRepository.save(vuelo);
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        Orden guardada = ordenRepository.save(orden);
        return toResponse(guardada);
    }

    private Orden buscarOrdenDelPasajero(Long usuarioId, Long ordenId) {
        Orden orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + ordenId));

        if (!orden.getUsuario().getId().equals(usuarioId)) {
            throw new ResourceNotFoundException("Orden no encontrada: " + ordenId);
        }

        return orden;
    }

    public OrdenResponse toResponse(Orden orden) {
        List<ItemOrdenResponse> items = orden.getItems().stream()
                .map(item -> ItemOrdenResponse.builder()
                        .id(item.getId())
                        .vueloId(item.getVuelo().getId())
                        .origen(item.getVuelo().getOrigen())
                        .destino(item.getVuelo().getDestino())
                        .cantidad(item.getCantidad())
                        .precioUnitario(item.getPrecioUnitario())
                        .subtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                        .build())
                .toList();

        return OrdenResponse.builder()
                .id(orden.getId())
                .total(orden.getTotal())
                .fecha(orden.getFecha())
                .estado(orden.getEstado())
                .items(items)
                .build();
    }
}
