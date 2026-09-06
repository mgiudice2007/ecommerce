package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.ItemOrden;
import com.ecommerce.vuelos.entity.Orden;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.DisponibilidadRepository;
import com.ecommerce.vuelos.repository.OrdenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrdenServiceImpl implements OrdenService {

    @Autowired
    private OrdenRepository ordenRepository;
    @Autowired
    private DisponibilidadRepository disponibilidadRepository;

    @Override
    public List<OrdenResponse> historial(Long usuarioId) {
        return ordenRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(OrdenResponse::desde)
                .toList();
    }

    @Override
    public OrdenResponse obtener(Long usuarioId, Long ordenId) {
        return OrdenResponse.desde(buscarOrdenDelUsuario(usuarioId, ordenId));
    }

    @Override
    @Transactional
    public OrdenResponse cancelar(Long usuarioId, Long ordenId) {
        Orden orden = buscarOrdenDelUsuario(usuarioId, ordenId);

        if (orden.getEstado() == EstadoOrden.CANCELADA) {
            throw new BadRequestException("La orden ya se encuentra cancelada");
        }

        // Cancelar devuelve los asientos al cupo del que salieron.
        for (ItemOrden item : orden.getItems()) {
            Disponibilidad disponibilidad = item.getDisponibilidad();
            disponibilidad.setAsientosDisponibles(
                    disponibilidad.getAsientosDisponibles() + item.getCantidad());
            disponibilidadRepository.save(disponibilidad);
        }

        orden.setEstado(EstadoOrden.CANCELADA);
        return OrdenResponse.desde(ordenRepository.save(orden));
    }

    private Orden buscarOrdenDelUsuario(Long usuarioId, Long ordenId) {
        Orden orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + ordenId));

        if (!orden.getUsuario().getId().equals(usuarioId)) {
            throw new ResourceNotFoundException("Orden no encontrada: " + ordenId);
        }

        return orden;
    }
}
