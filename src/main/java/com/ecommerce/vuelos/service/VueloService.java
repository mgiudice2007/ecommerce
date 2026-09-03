package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.VueloRequest;
import com.ecommerce.vuelos.dto.vuelo.VueloResponse;
import com.ecommerce.vuelos.entity.Aeropuerto;
import com.ecommerce.vuelos.entity.Categoria;
import com.ecommerce.vuelos.entity.ClaseVuelo;
import com.ecommerce.vuelos.entity.EstadoVuelo;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.AeropuertoRepository;
import com.ecommerce.vuelos.repository.CategoriaRepository;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VueloService {

    private final VueloRepository vueloRepository;
    private final CategoriaRepository categoriaRepository;
    private final AeropuertoRepository aeropuertoRepository;
    private final UsuarioRepository usuarioRepository;

    public List<VueloResponse> buscar(String origen, String destino, Long categoriaId, ClaseVuelo clase,
                                      BigDecimal precioMin, BigDecimal precioMax, Long vendedorId) {
        Specification<Vuelo> spec = Specification.allOf(
                VueloSpecifications.soloPublicados(),
                VueloSpecifications.origenContiene(origen),
                VueloSpecifications.destinoContiene(destino),
                VueloSpecifications.deCategoria(categoriaId),
                VueloSpecifications.esClase(clase),
                VueloSpecifications.precioMinimo(precioMin),
                VueloSpecifications.precioMaximo(precioMax),
                VueloSpecifications.delVendedor(vendedorId));

        return vueloRepository.findAll(spec).stream()
                .map(this::toResponse)
                .toList();
    }

    public VueloResponse obtener(Long id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public VueloResponse crear(VueloRequest request, Long vendedorId) {
        Usuario vendedor = usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + vendedorId));

        Vuelo vuelo = Vuelo.builder()
                .vendedor(vendedor)
                .categoria(buscarCategoria(request.getCategoriaId()))
                .origen(buscarAeropuerto(request.getOrigenIata()))
                .destino(buscarAeropuerto(request.getDestinoIata()))
                .numeroVuelo(request.getNumeroVuelo())
                .descripcion(request.getDescripcion())
                .fechaSalida(request.getFechaSalida())
                .fechaLlegada(request.getFechaLlegada())
                .precio(request.getPrecio())
                .asientosDisponibles(request.getAsientosDisponibles())
                .descuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO)
                .clase(request.getClase())
                .estado(EstadoVuelo.ACTIVO)
                .fechaAlta(LocalDateTime.now())
                .build();

        validarFechas(vuelo);
        return toResponse(vueloRepository.save(vuelo));
    }

    @Transactional
    public VueloResponse actualizar(Long id, VueloRequest request, UsuarioPrincipal principal) {
        Vuelo vuelo = buscarPorId(id);
        validarPropiedad(vuelo, principal);

        vuelo.setCategoria(buscarCategoria(request.getCategoriaId()));
        vuelo.setOrigen(buscarAeropuerto(request.getOrigenIata()));
        vuelo.setDestino(buscarAeropuerto(request.getDestinoIata()));
        vuelo.setNumeroVuelo(request.getNumeroVuelo());
        vuelo.setDescripcion(request.getDescripcion());
        vuelo.setFechaSalida(request.getFechaSalida());
        vuelo.setFechaLlegada(request.getFechaLlegada());
        vuelo.setPrecio(request.getPrecio());
        vuelo.setAsientosDisponibles(request.getAsientosDisponibles());
        vuelo.setDescuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO);
        vuelo.setClase(request.getClase());

        validarFechas(vuelo);
        return toResponse(vueloRepository.save(vuelo));
    }

    /**
     * Baja logica: el vuelo queda en ELIMINADO en vez de borrarse, para no
     * romper las ordenes que ya lo referencian.
     */
    @Transactional
    public void eliminar(Long id, UsuarioPrincipal principal) {
        Vuelo vuelo = buscarPorId(id);
        validarPropiedad(vuelo, principal);

        vuelo.setEstado(EstadoVuelo.ELIMINADO);
        vuelo.setFechaBaja(LocalDateTime.now());
        vueloRepository.save(vuelo);
    }

    /** Solo el vendedor que publico el vuelo, o un ADMIN, pueden tocarlo. */
    private void validarPropiedad(Vuelo vuelo, UsuarioPrincipal principal) {
        if (principal.esAdmin()) {
            return;
        }
        if (!vuelo.getVendedor().getId().equals(principal.getId())) {
            throw new BadRequestException("Solo el vendedor que publico el vuelo puede modificarlo");
        }
    }

    private void validarFechas(Vuelo vuelo) {
        if (!vuelo.getFechaLlegada().isAfter(vuelo.getFechaSalida())) {
            throw new BadRequestException("La fecha de llegada debe ser posterior a la de salida");
        }
    }

    private Vuelo buscarPorId(Long id) {
        return vueloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vuelo no encontrado: " + id));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada: " + id));
    }

    private Aeropuerto buscarAeropuerto(String iata) {
        return aeropuertoRepository.findById(iata.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Aeropuerto no encontrado: " + iata));
    }

    private VueloResponse toResponse(Vuelo vuelo) {
        return VueloResponse.builder()
                .id(vuelo.getId())
                .numeroVuelo(vuelo.getNumeroVuelo())
                .descripcion(vuelo.getDescripcion())
                .categoriaId(vuelo.getCategoria().getId())
                .categoriaNombre(vuelo.getCategoria().getNombre())
                .origenIata(vuelo.getOrigen().getCodigoIata())
                .origenCiudad(vuelo.getOrigen().getCiudad())
                .destinoIata(vuelo.getDestino().getCodigoIata())
                .destinoCiudad(vuelo.getDestino().getCiudad())
                .fechaSalida(vuelo.getFechaSalida())
                .fechaLlegada(vuelo.getFechaLlegada())
                .duracionMinutos(vuelo.getDuracionMinutos())
                .precio(vuelo.getPrecio())
                .descuento(vuelo.getDescuento())
                .precioConDescuento(vuelo.getPrecioConDescuento())
                .asientosDisponibles(vuelo.getAsientosDisponibles())
                .hayStock(vuelo.isDisponible())
                .clase(vuelo.getClase())
                .estado(vuelo.getEstado())
                .vendedorId(vuelo.getVendedor().getId())
                .vendedorUsername(vuelo.getVendedor().getUsername())
                .build();
    }
}
