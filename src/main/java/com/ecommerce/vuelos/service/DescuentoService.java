package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.descuento.DescuentoRequest;
import com.ecommerce.vuelos.dto.descuento.DescuentoResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;

import java.util.List;

public interface DescuentoService {

    List<DescuentoResponse> listarPorVuelo(Long vueloId);

    DescuentoResponse obtener(Long id);

    DescuentoResponse crear(DescuentoRequest request, UsuarioPrincipal principal);

    DescuentoResponse actualizar(Long id, DescuentoRequest request, UsuarioPrincipal principal);

    void eliminar(Long id, UsuarioPrincipal principal);
}
