package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.OrdenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPRADOR')")
public class OrdenController {

    private final OrdenService ordenService;

    @GetMapping
    public ResponseEntity<List<OrdenResponse>> historial(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(ordenService.historial(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponse> obtener(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                     @PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtener(principal.getId(), id));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<OrdenResponse> cancelar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(ordenService.cancelar(principal.getId(), id));
    }
}
