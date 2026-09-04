package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.vuelo.DisponibilidadRequest;
import com.ecommerce.vuelos.dto.vuelo.DisponibilidadResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.DisponibilidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    @GetMapping("/api/disponibilidades")
    public ResponseEntity<List<DisponibilidadResponse>> listar(@RequestParam Long vueloId) {
        return ResponseEntity.ok(disponibilidadService.listarPorVuelo(vueloId));
    }

    @GetMapping("/api/disponibilidades/{id}")
    public ResponseEntity<DisponibilidadResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(disponibilidadService.obtener(id));
    }

    @PostMapping("/api/disponibilidades")
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<DisponibilidadResponse> crear(@Valid @RequestBody DisponibilidadRequest request,
                                                        @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disponibilidadService.crear(request, principal));
    }

    @PutMapping("/api/disponibilidades/{id}")
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<DisponibilidadResponse> actualizar(@PathVariable Long id,
                                                             @Valid @RequestBody DisponibilidadRequest request,
                                                             @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(disponibilidadService.actualizar(id, request, principal));
    }
}
