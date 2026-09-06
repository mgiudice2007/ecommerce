package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.catalogo.AeropuertoResponse;
import com.ecommerce.vuelos.dto.catalogo.CategoriaResponse;
import com.ecommerce.vuelos.dto.catalogo.ClaseResponse;
import com.ecommerce.vuelos.service.CatalogoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CatalogoController {

    @Autowired
    private CatalogoService catalogoService;

    @GetMapping("/api/categorias")
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        return ResponseEntity.ok(catalogoService.listarCategorias());
    }

    @GetMapping("/api/aeropuertos")
    public ResponseEntity<List<AeropuertoResponse>> listarAeropuertos() {
        return ResponseEntity.ok(catalogoService.listarAeropuertos());
    }

    @GetMapping("/api/clases")
    public ResponseEntity<List<ClaseResponse>> listarClases() {
        return ResponseEntity.ok(catalogoService.listarClases());
    }
}
