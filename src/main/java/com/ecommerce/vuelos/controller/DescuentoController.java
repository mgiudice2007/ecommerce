package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.vuelo.DescuentoRequest;
import com.ecommerce.vuelos.dto.vuelo.DescuentoResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.DescuentoService;
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
public class DescuentoController {

    private final DescuentoService descuentoService;

    @GetMapping("/api/descuentos")
    public ResponseEntity<List<DescuentoResponse>> listar(@RequestParam Long vueloId) {
        return ResponseEntity.ok(descuentoService.listarPorVuelo(vueloId));
    }

    @GetMapping("/api/descuentos/{id}")
    public ResponseEntity<DescuentoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(descuentoService.obtener(id));
    }

    @PostMapping("/api/descuentos")
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<DescuentoResponse> crear(@Valid @RequestBody DescuentoRequest request,
                                                    @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(descuentoService.crear(request, principal));
    }

    @PutMapping("/api/descuentos/{id}")
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<DescuentoResponse> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody DescuentoRequest request,
                                                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(descuentoService.actualizar(id, request, principal));
    }
}
