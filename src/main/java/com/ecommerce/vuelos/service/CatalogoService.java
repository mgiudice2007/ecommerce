package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.catalogo.AeropuertoResponse;
import com.ecommerce.vuelos.dto.catalogo.CategoriaResponse;
import com.ecommerce.vuelos.dto.catalogo.ClaseResponse;

import java.util.List;

/**
 * Las tablas de consulta del sistema. Son de solo lectura: el cliente las lee
 * para saber que ids mandar al publicar un vuelo o al cargar un cupo.
 */
public interface CatalogoService {

    List<CategoriaResponse> listarCategorias();

    List<AeropuertoResponse> listarAeropuertos();

    List<ClaseResponse> listarClases();
}
