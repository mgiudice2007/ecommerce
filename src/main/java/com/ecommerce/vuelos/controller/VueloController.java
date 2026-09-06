package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.vuelo.VueloRequest;
import com.ecommerce.vuelos.dto.vuelo.VueloResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.VueloService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/vuelos")
public class VueloController {

    @Autowired
    private VueloService vueloService;

    /**
     * Catalogo publico. Si no mandan page y size devuelve todo en una sola
     * pagina; si los mandan, pagina de verdad.
     */
    @GetMapping
    public ResponseEntity<Page<VueloResponse>> buscar(
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long claseId,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) Long vendedorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        PageRequest pageRequest = (page == null || size == null)
                ? PageRequest.of(0, Integer.MAX_VALUE)
                : PageRequest.of(page, size);

        return ResponseEntity.ok(vueloService.buscar(
                origen, destino, categoriaId, claseId, precioMin, precioMax, vendedorId, pageRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VueloResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(vueloService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<VueloResponse> crear(@Valid @RequestBody VueloRequest request,
                                               @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vueloService.crear(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<VueloResponse> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody VueloRequest request,
                                                    @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(vueloService.actualizar(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        vueloService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }
}
