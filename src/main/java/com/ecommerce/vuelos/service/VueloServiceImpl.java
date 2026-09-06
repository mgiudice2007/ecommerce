package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.VueloRequest;
import com.ecommerce.vuelos.dto.vuelo.VueloResponse;
import com.ecommerce.vuelos.entity.Aeropuerto;
import com.ecommerce.vuelos.entity.Categoria;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class VueloServiceImpl implements VueloService {

    @Autowired
    private VueloRepository vueloRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private AeropuertoRepository aeropuertoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Page<VueloResponse> buscar(String origen, String destino, Long categoriaId, Long claseId,
                                      BigDecimal precioMin, BigDecimal precioMax, Long vendedorId,
                                      PageRequest pageRequest) {
        // Cada Specification devuelve null si su filtro vino vacio, asi que las
        // combinamos todas y quedan solo las que el cliente realmente mando.
        Specification<Vuelo> spec = Specification.allOf(
                VueloSpecifications.soloPublicados(),
                VueloSpecifications.origenContiene(origen),
                VueloSpecifications.destinoContiene(destino),
                VueloSpecifications.deCategoria(categoriaId),
                VueloSpecifications.deClase(claseId),
                VueloSpecifications.precioMinimo(precioMin),
                VueloSpecifications.precioMaximo(precioMax),
                VueloSpecifications.delVendedor(vendedorId));

        return vueloRepository.findAll(spec, pageRequest).map(VueloResponse::desde);
    }

    @Override
    public VueloResponse obtener(Long id) {
        return VueloResponse.desde(buscarPorId(id));
    }

    @Override
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
                .estado(EstadoVuelo.ACTIVO)
                .fechaAlta(LocalDateTime.now())
                .build();

        validarFechas(vuelo);
        return VueloResponse.desde(vueloRepository.save(vuelo));
    }

    @Override
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

        validarFechas(vuelo);
        return VueloResponse.desde(vueloRepository.save(vuelo));
    }

    /**
     * Baja logica: el vuelo queda en ELIMINADO en vez de borrarse, para no
     * romper las ordenes que ya lo referencian.
     */
    @Override
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
}
