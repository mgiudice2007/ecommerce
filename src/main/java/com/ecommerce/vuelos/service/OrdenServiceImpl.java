package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.ItemOrden;
import com.ecommerce.vuelos.entity.Orden;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.DisponibilidadRepository;
import com.ecommerce.vuelos.repository.OrdenRepository;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrdenServiceImpl implements OrdenService {

    @Autowired
    private OrdenRepository ordenRepository;
    @Autowired
    private DisponibilidadRepository disponibilidadRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Hasta cuantas horas antes del vuelo se puede cancelar (application.properties)
    @Value("${ordenes.horas-para-cancelar}")
    private int horasParaCancelar;

    @Override
    public List<OrdenResponse> historial(Long usuarioId) {
        return ordenRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map((orden) -> OrdenResponse.desde(orden, horasParaCancelar))
                .toList();
    }

    @Override
    public OrdenResponse obtener(Long usuarioId, Long ordenId) {
        return OrdenResponse.desde(buscarOrdenDelUsuario(usuarioId, ordenId), horasParaCancelar);
    }

    @Override
    @Transactional
    public OrdenResponse cancelar(Long usuarioId, Long ordenId) {
        Orden orden = buscarOrdenDelUsuario(usuarioId, ordenId);

        if (orden.getEstado() == EstadoOrden.CANCELADA) {
            throw new BadRequestException("La orden ya se encuentra cancelada");
        }

        // Como en las aerolineas: solo hasta 24 horas antes de que salga el primer vuelo
        LocalDateTime primeraSalida = orden.primeraSalida();
        if (primeraSalida.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("No se puede cancelar la compra: el vuelo ya salió");
        }
        if (LocalDateTime.now().isAfter(primeraSalida.minusHours(horasParaCancelar))) {
            throw new BadRequestException("No se puede cancelar la compra: faltan menos de "
                    + horasParaCancelar + " horas para la salida del vuelo");
        }

        // Cancelar devuelve los asientos al cupo del que salieron (los bebés no ocupaban)
        // y libera los asientos elegidos para que otro los pueda comprar.
        for (ItemOrden item : orden.getItems()) {
            if (item.getTipoPasajero().ocupaAsiento()) {
                Disponibilidad disponibilidad = item.getDisponibilidad();
                disponibilidad.setAsientosDisponibles(
                        disponibilidad.getAsientosDisponibles() + item.getCantidad());
                disponibilidadRepository.save(disponibilidad);
            }
            item.getAsientos().clear();
        }

        // Devuelve las millas que uso y le saca las que habia ganado con esta compra
        Usuario usuario = orden.getUsuario();
        int saldo = usuario.getMillas() + orden.getMillasUsadas() - orden.getMillasGanadas();
        usuario.setMillas(Math.max(saldo, 0));
        usuarioRepository.save(usuario);

        orden.setEstado(EstadoOrden.CANCELADA);
        return OrdenResponse.desde(ordenRepository.save(orden), horasParaCancelar);
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
