package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.PaginaResponse;
import com.ecommerce.vuelos.dto.vuelo.EstadoVueloResponse;
import com.ecommerce.vuelos.dto.vuelo.VueloRequest;
import com.ecommerce.vuelos.dto.vuelo.VueloResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.entity.EstadoVuelo;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface VueloService {

    /** Catalogo publico con filtros opcionales. Todos los filtros pueden venir en null. */
    PaginaResponse<VueloResponse> buscar(String origen, String destino, Long categoriaId, Long claseId,
                               BigDecimal precioMin, BigDecimal precioMax,
                                         LocalDate fechaDesde, LocalDate fechaHasta, Long vendedorId,
                               PageRequest pageRequest);

    VueloResponse obtener(Long id);

    VueloResponse crear(VueloRequest request, Long vendedorId);

    VueloResponse actualizar(Long id, VueloRequest request, UsuarioPrincipal principal);

    /** Cambia el estado del vuelo (demorado, pausado, cancelado, eliminado) sin tocar el resto de sus datos. */
    EstadoVueloResponse cambiarEstado(Long id, EstadoVuelo nuevoEstado, UsuarioPrincipal principal);
}
