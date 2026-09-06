package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.catalogo.AeropuertoResponse;
import com.ecommerce.vuelos.dto.catalogo.CategoriaResponse;
import com.ecommerce.vuelos.dto.catalogo.ClaseResponse;
import com.ecommerce.vuelos.repository.AeropuertoRepository;
import com.ecommerce.vuelos.repository.CategoriaRepository;
import com.ecommerce.vuelos.repository.ClaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogoServiceImpl implements CatalogoService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private AeropuertoRepository aeropuertoRepository;

    @Autowired
    private ClaseRepository claseRepository;

    @Override
    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepository.findAll().stream()
                .map(CategoriaResponse::desde)
                .toList();
    }

    @Override
    public List<AeropuertoResponse> listarAeropuertos() {
        return aeropuertoRepository.findAll().stream()
                .map(AeropuertoResponse::desde)
                .toList();
    }

    @Override
    public List<ClaseResponse> listarClases() {
        return claseRepository.findAll().stream()
                .map(ClaseResponse::desde)
                .toList();
    }
}
