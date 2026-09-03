package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.aerolinea.AerolineaRequest;
import com.ecommerce.vuelos.dto.aerolinea.AerolineaResponse;
import com.ecommerce.vuelos.service.AerolineaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aerolineas")
@RequiredArgsConstructor
public class AerolineaController {

    private final AerolineaService aerolineaService;

    @GetMapping
    public ResponseEntity<List<AerolineaResponse>> listar() {
        return ResponseEntity.ok(aerolineaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AerolineaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(aerolineaService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AerolineaResponse> crear(@Valid @RequestBody AerolineaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aerolineaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AerolineaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody AerolineaRequest request) {
        return ResponseEntity.ok(aerolineaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        aerolineaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
