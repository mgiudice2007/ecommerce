package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.DescuentoRequest;
import com.ecommerce.vuelos.dto.vuelo.DescuentoResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;

import java.util.List;

/** Promociones por vuelo. La vigente hoy es la que define el precio final. */
public interface DescuentoService {

    List<DescuentoResponse> listarPorVuelo(Long vueloId);

    DescuentoResponse obtener(Long id);

    DescuentoResponse crear(DescuentoRequest request, UsuarioPrincipal principal);

    DescuentoResponse actualizar(Long id, DescuentoRequest request, UsuarioPrincipal principal);
}
