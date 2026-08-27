package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.aerolinea.AerolineaRequest;
import com.ecommerce.vuelos.dto.aerolinea.AerolineaResponse;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.model.Aerolinea;
import com.ecommerce.vuelos.repository.AerolineaRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AerolineaService {

    private final AerolineaRepository aerolineaRepository;
    private final VueloRepository vueloRepository;

    public List<AerolineaResponse> listar() {
        return aerolineaRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AerolineaResponse obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public AerolineaResponse crear(AerolineaRequest request) {
        if (aerolineaRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new BadRequestException("Ya existe una aerolinea con ese nombre");
        }
        Aerolinea aerolinea = Aerolinea.builder()
                .nombre(request.getNombre())
                .build();
        return toResponse(aerolineaRepository.save(aerolinea));
    }

    @Transactional
    public AerolineaResponse actualizar(Long id, AerolineaRequest request) {
        Aerolinea aerolinea = buscarPorId(id);
        aerolinea.setNombre(request.getNombre());
        return toResponse(aerolineaRepository.save(aerolinea));
    }

    @Transactional
    public void eliminar(Long id) {
        Aerolinea aerolinea = buscarPorId(id);
        if (vueloRepository.existsByAerolineaId(id)) {
            throw new BadRequestException("No se puede eliminar una aerolinea con vuelos asociados");
        }
        aerolineaRepository.delete(aerolinea);
    }

    private Aerolinea buscarPorId(Long id) {
        return aerolineaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aerolinea no encontrada: " + id));
    }

    private AerolineaResponse toResponse(Aerolinea aerolinea) {
        return AerolineaResponse.builder()
                .id(aerolinea.getId())
                .nombre(aerolinea.getNombre())
                .cantidadVuelos(aerolinea.getVuelos().size())
                .build();
    }
}
