package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.carrito.ActualizarCantidadRequest;
import com.ecommerce.vuelos.dto.carrito.CarritoResponse;
import com.ecommerce.vuelos.dto.carrito.ItemCarritoRequest;
import com.ecommerce.vuelos.dto.reserva.ReservaResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.CarritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PASAJERO')")
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping
    public ResponseEntity<CarritoResponse> obtener(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(carritoService.obtenerCarrito(principal.getId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregarItem(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                         @Valid @RequestBody ItemCarritoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carritoService.agregarItem(principal.getId(), request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> actualizarItem(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                            @PathVariable Long itemId,
                                                            @Valid @RequestBody ActualizarCantidadRequest request) {
        return ResponseEntity.ok(carritoService.actualizarItem(principal.getId(), itemId, request.getCantidad()));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> eliminarItem(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                          @PathVariable Long itemId) {
        return ResponseEntity.ok(carritoService.eliminarItem(principal.getId(), itemId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ReservaResponse> checkout(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carritoService.checkout(principal.getId()));
    }
}
