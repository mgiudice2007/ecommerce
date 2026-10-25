package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.vuelo.DisponibilidadRequest;
import com.ecommerce.vuelos.dto.vuelo.DisponibilidadResponse;
import com.ecommerce.vuelos.dto.vuelo.MapaAsientosResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.AsientoService;
import com.ecommerce.vuelos.service.DisponibilidadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DisponibilidadController {

    @Autowired
    private DisponibilidadService disponibilidadService;
    @Autowired
    private AsientoService asientoService;

    @GetMapping("/api/disponibilidades")
    public ResponseEntity<List<DisponibilidadResponse>> listar(@RequestParam Long vueloId) {
        return ResponseEntity.ok(disponibilidadService.listarPorVuelo(vueloId));
    }

    @GetMapping("/api/disponibilidades/{id}")
    public ResponseEntity<DisponibilidadResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(disponibilidadService.obtener(id));
    }

    /** El mapa de asientos de un vuelo y clase, con los que ya estan vendidos. */
    @GetMapping("/api/disponibilidades/{id}/asientos")
    public ResponseEntity<MapaAsientosResponse> asientos(@PathVariable Long id) {
        return ResponseEntity.ok(asientoService.mapa(id));
    }

    @PostMapping("/api/disponibilidades")
    public ResponseEntity<DisponibilidadResponse> crear(@Valid @RequestBody DisponibilidadRequest request,
                                                        @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disponibilidadService.crear(request, principal));
    }

    @PutMapping("/api/disponibilidades/{id}")
    public ResponseEntity<DisponibilidadResponse> actualizar(@PathVariable Long id,
                                                             @Valid @RequestBody DisponibilidadRequest request,
                                                             @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(disponibilidadService.actualizar(id, request, principal));
    }

    /** Saca una clase del vuelo (solo si nunca se vendio). */
    @DeleteMapping("/api/disponibilidades/{id}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id,
                                                        @AuthenticationPrincipal UsuarioPrincipal principal) {
        disponibilidadService.eliminar(id, principal);
        return ResponseEntity.ok(Map.of("mensaje", "Clase eliminada del vuelo"));
    }
}
