package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.reserva.ReservaResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PASAJERO')")
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping
    public ResponseEntity<List<ReservaResponse>> historial(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(reservaService.historial(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> obtener(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                     @PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtener(principal.getId(), id));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelar(principal.getId(), id));
    }
}
