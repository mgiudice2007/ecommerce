package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.carrito.ActualizarCantidadRequest;
import com.ecommerce.vuelos.dto.carrito.CarritoResponse;
import com.ecommerce.vuelos.dto.carrito.ItemCarritoRequest;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.CarritoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
@PreAuthorize("hasRole('COMPRADOR')")
public class CarritoController {

    @Autowired
    private CarritoService carritoService;

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
    public ResponseEntity<OrdenResponse> checkout(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carritoService.checkout(principal.getId()));
    }
}
