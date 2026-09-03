package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.ClaseResponse;
import com.ecommerce.vuelos.dto.vuelo.DisponibilidadRequest;
import com.ecommerce.vuelos.dto.vuelo.DisponibilidadResponse;
import com.ecommerce.vuelos.entity.Clase;
import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.ClaseRepository;
import com.ecommerce.vuelos.repository.DisponibilidadRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadServiceImpl implements DisponibilidadService {

    private final DisponibilidadRepository disponibilidadRepository;
    private final VueloRepository vueloRepository;
    private final ClaseRepository claseRepository;

    @Override
    public List<ClaseResponse> listarClases() {
        return claseRepository.findAll().stream()
                .map(c -> ClaseResponse.builder()
                        .id(c.getId())
                        .nombre(c.getNombre())
                        .descripcion(c.getDescripcion())
                        .equipajeBodega(c.getEquipajeBodega())
                        .build())
                .toList();
    }

    @Override
    public List<DisponibilidadResponse> listarPorVuelo(Long vueloId) {
        return disponibilidadRepository.findByVueloId(vueloId).stream()
                .map(DisponibilidadResponse::desde)
                .toList();
    }

    @Override
    public DisponibilidadResponse obtener(Long id) {
        return DisponibilidadResponse.desde(buscarPorId(id));
    }

    @Override
    @Transactional
    public DisponibilidadResponse crear(DisponibilidadRequest request, UsuarioPrincipal principal) {
        Vuelo vuelo = buscarVuelo(request.getVueloId());
        validarPropiedad(vuelo, principal);

        if (disponibilidadRepository.existsByVueloIdAndClaseId(vuelo.getId(), request.getClaseId())) {
            throw new BadRequestException("Ese vuelo ya tiene cargado un cupo para esa clase");
        }

        Clase clase = claseRepository.findById(request.getClaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Clase no encontrada: " + request.getClaseId()));

        Disponibilidad disponibilidad = Disponibilidad.builder()
                .vuelo(vuelo)
                .clase(clase)
                .asientosTotales(request.getAsientosTotales())
                .asientosDisponibles(request.getAsientosTotales())
                .precio(request.getPrecio())
                .build();

        Disponibilidad guardada = disponibilidadRepository.save(disponibilidad);

        // Hay que mantener los dos lados de la relacion: el vuelo tiene la coleccion
        // en memoria y sin esto queda desactualizada dentro de la misma transaccion.
        vuelo.getDisponibilidades().add(guardada);

        return DisponibilidadResponse.desde(guardada);
    }

    /** El manejo del stock del enunciado: el vendedor ajusta asientos y precio. */
    @Override
    @Transactional
    public DisponibilidadResponse actualizar(Long id, DisponibilidadRequest request, UsuarioPrincipal principal) {
        Disponibilidad disponibilidad = buscarPorId(id);
        validarPropiedad(disponibilidad.getVuelo(), principal);

        int vendidos = disponibilidad.getAsientosTotales() - disponibilidad.getAsientosDisponibles();
        if (request.getAsientosTotales() < vendidos) {
            throw new BadRequestException(
                    "No se puede bajar el total a " + request.getAsientosTotales()
                            + ": ya hay " + vendidos + " asientos vendidos");
        }

        disponibilidad.setAsientosTotales(request.getAsientosTotales());
        disponibilidad.setAsientosDisponibles(request.getAsientosTotales() - vendidos);
        disponibilidad.setPrecio(request.getPrecio());

        return DisponibilidadResponse.desde(disponibilidadRepository.save(disponibilidad));
    }

    private Disponibilidad buscarPorId(Long id) {
        return disponibilidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidad no encontrada: " + id));
    }

    private Vuelo buscarVuelo(Long id) {
        return vueloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vuelo no encontrado: " + id));
    }

    private void validarPropiedad(Vuelo vuelo, UsuarioPrincipal principal) {
        if (principal.esAdmin()) {
            return;
        }
        if (!vuelo.getVendedor().getId().equals(principal.getId())) {
            throw new BadRequestException("Solo el vendedor que publico el vuelo puede manejar su stock");
        }
    }
}
