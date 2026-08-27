package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.VueloRequest;
import com.ecommerce.vuelos.dto.vuelo.VueloResponse;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.model.Aerolinea;
import com.ecommerce.vuelos.model.ClaseVuelo;
import com.ecommerce.vuelos.model.Vuelo;
import com.ecommerce.vuelos.repository.AerolineaRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VueloService {

    private final VueloRepository vueloRepository;
    private final AerolineaRepository aerolineaRepository;

    public List<VueloResponse> buscar(String origen, String destino, ClaseVuelo clase,
                                       BigDecimal precioMin, BigDecimal precioMax) {
        Specification<Vuelo> spec = Specification.allOf(
                VueloSpecifications.origenContiene(origen),
                VueloSpecifications.destinoContiene(destino),
                VueloSpecifications.esClase(clase),
                VueloSpecifications.precioMinimo(precioMin),
                VueloSpecifications.precioMaximo(precioMax)
        );
        return vueloRepository.findAll(spec).stream()
                .map(this::toResponse)
                .toList();
    }

    public VueloResponse obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public VueloResponse crear(VueloRequest request) {
        Aerolinea aerolinea = buscarAerolinea(request.getAerolineaId());
        Vuelo vuelo = Vuelo.builder()
                .origen(request.getOrigen())
                .destino(request.getDestino())
                .fechaSalida(request.getFechaSalida())
                .precio(request.getPrecio())
                .asientosDisponibles(request.getAsientosDisponibles())
                .descuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO)
                .clase(request.getClase())
                .aerolinea(aerolinea)
                .build();
        return toResponse(vueloRepository.save(vuelo));
    }

    @Transactional
    public VueloResponse actualizar(Long id, VueloRequest request) {
        Vuelo vuelo = buscarPorId(id);
        Aerolinea aerolinea = buscarAerolinea(request.getAerolineaId());

        vuelo.setOrigen(request.getOrigen());
        vuelo.setDestino(request.getDestino());
        vuelo.setFechaSalida(request.getFechaSalida());
        vuelo.setPrecio(request.getPrecio());
        vuelo.setAsientosDisponibles(request.getAsientosDisponibles());
        vuelo.setDescuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO);
        vuelo.setClase(request.getClase());
        vuelo.setAerolinea(aerolinea);

        return toResponse(vueloRepository.save(vuelo));
    }

    @Transactional
    public void eliminar(Long id) {
        Vuelo vuelo = buscarPorId(id);
        vueloRepository.delete(vuelo);
    }

    private Vuelo buscarPorId(Long id) {
        return vueloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vuelo no encontrado: " + id));
    }

    private Aerolinea buscarAerolinea(Long id) {
        return aerolineaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aerolinea no encontrada: " + id));
    }

    private VueloResponse toResponse(Vuelo vuelo) {
        return VueloResponse.builder()
                .id(vuelo.getId())
                .origen(vuelo.getOrigen())
                .destino(vuelo.getDestino())
                .fechaSalida(vuelo.getFechaSalida())
                .precio(vuelo.getPrecio())
                .descuento(vuelo.getDescuento())
                .precioConDescuento(vuelo.getPrecioConDescuento())
                .asientosDisponibles(vuelo.getAsientosDisponibles())
                .clase(vuelo.getClase())
                .aerolineaId(vuelo.getAerolinea().getId())
                .aerolineaNombre(vuelo.getAerolinea().getNombre())
                .build();
    }
}
