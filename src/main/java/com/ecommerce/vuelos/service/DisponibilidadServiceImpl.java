package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.DisponibilidadRequest;
import com.ecommerce.vuelos.dto.vuelo.DisponibilidadResponse;
import com.ecommerce.vuelos.entity.Clase;
import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.ItemCarrito;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.ClaseRepository;
import com.ecommerce.vuelos.repository.DisponibilidadRepository;
import com.ecommerce.vuelos.repository.ItemCarritoRepository;
import com.ecommerce.vuelos.repository.ItemOrdenRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DisponibilidadServiceImpl implements DisponibilidadService {

    @Autowired
    private DisponibilidadRepository disponibilidadRepository;
    @Autowired
    private VueloRepository vueloRepository;
    @Autowired
    private ClaseRepository claseRepository;
    @Autowired
    private ItemOrdenRepository itemOrdenRepository;
    @Autowired
    private ItemCarritoRepository itemCarritoRepository;

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

    //carga un cupo nuevo (asientos + precio) para una clase de un vuelo
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
        validarCantidadAsientos(clase, request.getAsientosTotales());
        validarPrecioEntreClases(vuelo, clase, request.getPrecio(), null);

        Disponibilidad disponibilidad = Disponibilidad.builder()
                .vuelo(vuelo)
                .clase(clase)
                .asientosTotales(request.getAsientosTotales())
                .asientosDisponibles(request.getAsientosTotales())
                .precio(request.getPrecio())
                .build();

        Disponibilidad guardada = disponibilidadRepository.save(disponibilidad);

        vuelo.getDisponibilidades().add(guardada);
        actualizarPrecioDelVuelo(vuelo);

        return DisponibilidadResponse.desde(guardada);
    }

    /** Manejo del stock: el vendedor ajusta asientos y precio. */
    @Override
    @Transactional
    public DisponibilidadResponse actualizar(Long id, DisponibilidadRequest request, UsuarioPrincipal principal) {
        Disponibilidad disponibilidad = buscarPorId(id);
        validarPropiedad(disponibilidad.getVuelo(), principal);
        validarCantidadAsientos(disponibilidad.getClase(), request.getAsientosTotales());
        validarPrecioEntreClases(disponibilidad.getVuelo(), disponibilidad.getClase(), request.getPrecio(), id);

        int vendidos = disponibilidad.getAsientosTotales() - disponibilidad.getAsientosDisponibles();
        if (request.getAsientosTotales() < vendidos) {
            throw new BadRequestException(
                    "No se puede bajar el total a " + request.getAsientosTotales()
                            + ": ya hay " + vendidos + " asientos vendidos");
        }

        disponibilidad.setAsientosTotales(request.getAsientosTotales());
        disponibilidad.setAsientosDisponibles(request.getAsientosTotales() - vendidos);
        disponibilidad.setPrecio(request.getPrecio());

        Disponibilidad guardada = disponibilidadRepository.save(disponibilidad);
        actualizarPrecioDelVuelo(disponibilidad.getVuelo());
        return DisponibilidadResponse.desde(guardada);
    }

    /**
     * Saca una clase del vuelo. Solo se puede si nunca se vendio: las compras (aunque
     * esten canceladas) quedan en el historial y apuntan a esta clase. Si estaba en
     * algun carrito, se saca de esos carritos.
     */
    @Override
    @Transactional
    public void eliminar(Long id, UsuarioPrincipal principal) {
        Disponibilidad disponibilidad = buscarPorId(id);
        Vuelo vuelo = disponibilidad.getVuelo();
        validarPropiedad(vuelo, principal);

        if (itemOrdenRepository.existsByDisponibilidadId(id)) {
            throw new BadRequestException("La clase " + disponibilidad.getClase().getNombre()
                    + " ya tiene compras (aunque esten canceladas) y no se puede eliminar");
        }

        for (ItemCarrito item : itemCarritoRepository.findByDisponibilidadId(id)) {
            item.getCarrito().getItems().remove(item);
            itemCarritoRepository.delete(item);
        }

        vuelo.getDisponibilidades().remove(disponibilidad);
        disponibilidadRepository.delete(disponibilidad);
        actualizarPrecioDelVuelo(vuelo);
    }

    /**
     * El precio del vuelo (el "desde" que se ve en la busqueda y en los filtros) es el
     * de su clase mas barata, asi el admin carga el precio una sola vez: en cada clase.
     */
    private void actualizarPrecioDelVuelo(Vuelo vuelo) {
        BigDecimal masBarato = null;
        for (Disponibilidad d : vuelo.getDisponibilidades()) {
            if (masBarato == null || d.getPrecio().compareTo(masBarato) < 0) {
                masBarato = d.getPrecio();
            }
        }
        if (masBarato != null) {
            vuelo.setPrecio(masBarato);
            vueloRepository.save(vuelo);
        }
    }

    /** Cada clase tiene un minimo y un maximo de asientos por vuelo, guardados en la tabla clases. */
    private void validarCantidadAsientos(Clase clase, int asientos) {
        boolean pocos = clase.getMinimoAsientos() != null && asientos < clase.getMinimoAsientos();
        boolean muchos = clase.getMaximoAsientos() != null && asientos > clase.getMaximoAsientos();
        if (pocos || muchos) {
            throw new BadRequestException("La clase " + clase.getNombre() + " tiene que tener entre "
                    + clase.getMinimoAsientos() + " y " + clase.getMaximoAsientos() + " asientos");
        }
    }

    /**
     * En un mismo vuelo una clase mejor no puede costar menos que una mas basica:
     * Economica <= Ejecutiva <= Primera (segun el nivel de cada clase).
     */
    private void validarPrecioEntreClases(Vuelo vuelo, Clase clase, BigDecimal precio, Long idQueSeEdita) {
        if (clase.getNivel() == null) {
            return;
        }
        for (Disponibilidad otra : vuelo.getDisponibilidades()) {
            Integer otroNivel = otra.getClase().getNivel();
            if (otra.getId().equals(idQueSeEdita) || otroNivel == null) {
                continue;
            }
            if (otroNivel < clase.getNivel() && precio.compareTo(otra.getPrecio()) < 0) {
                throw new BadRequestException("La clase " + clase.getNombre() + " no puede costar menos que "
                        + otra.getClase().getNombre() + " ($" + otra.getPrecio().toBigInteger() + ")");
            }
            if (otroNivel > clase.getNivel() && precio.compareTo(otra.getPrecio()) > 0) {
                throw new BadRequestException("La clase " + clase.getNombre() + " no puede costar mas que "
                        + otra.getClase().getNombre() + " ($" + otra.getPrecio().toBigInteger() + ")");
            }
        }
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
