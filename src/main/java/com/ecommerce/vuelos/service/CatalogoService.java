package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.catalogo.AeropuertoResponse;
import com.ecommerce.vuelos.dto.catalogo.CategoriaResponse;
import com.ecommerce.vuelos.dto.catalogo.ClaseResponse;

import java.util.List;

public interface CatalogoService {

    List<CategoriaResponse> listarCategorias();

    List<AeropuertoResponse> listarAeropuertos();

    List<ClaseResponse> listarClases();
}
