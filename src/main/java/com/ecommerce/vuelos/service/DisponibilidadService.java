package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.DisponibilidadRequest;
import com.ecommerce.vuelos.dto.vuelo.DisponibilidadResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;

import java.util.List;

/** Manejo del stock: cuantos asientos hay de cada clase en cada vuelo. */
public interface DisponibilidadService {

    List<DisponibilidadResponse> listarPorVuelo(Long vueloId);

    DisponibilidadResponse obtener(Long id);

    DisponibilidadResponse crear(DisponibilidadRequest request, UsuarioPrincipal principal);

    DisponibilidadResponse actualizar(Long id, DisponibilidadRequest request, UsuarioPrincipal principal);
}
