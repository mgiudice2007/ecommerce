package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.VueloRequest;
import com.ecommerce.vuelos.dto.vuelo.VueloResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

public interface VueloService {

    /** Catalogo publico con filtros opcionales. Todos los filtros pueden venir en null. */
    Page<VueloResponse> buscar(String origen, String destino, Long categoriaId, Long claseId,
                               BigDecimal precioMin, BigDecimal precioMax, Long vendedorId,
                               PageRequest pageRequest);

    VueloResponse obtener(Long id);

    VueloResponse crear(VueloRequest request, Long vendedorId);

    VueloResponse actualizar(Long id, VueloRequest request, UsuarioPrincipal principal);

    void eliminar(Long id, UsuarioPrincipal principal);
}
