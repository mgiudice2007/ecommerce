package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.vuelo.VueloRequest;
import com.ecommerce.vuelos.dto.vuelo.VueloResponse;
import com.ecommerce.vuelos.entity.ClaseVuelo;
import com.ecommerce.vuelos.service.VueloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vuelos")
@RequiredArgsConstructor
public class VueloController {

    private final VueloService vueloService;

    @GetMapping
    public ResponseEntity<List<VueloResponse>> buscar(
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) ClaseVuelo clase,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax) {
        return ResponseEntity.ok(vueloService.buscar(origen, destino, clase, precioMin, precioMax));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VueloResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(vueloService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<VueloResponse> crear(@Valid @RequestBody VueloRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vueloService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<VueloResponse> actualizar(@PathVariable Long id, @Valid @RequestBody VueloRequest request) {
        return ResponseEntity.ok(vueloService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        vueloService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
