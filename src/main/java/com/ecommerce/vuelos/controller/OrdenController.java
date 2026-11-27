package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.OrdenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    @Autowired
    private OrdenService ordenService;

    @GetMapping
    public ResponseEntity<List<OrdenResponse>> historial(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(ordenService.historial(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponse> obtener(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                     @PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtener(principal.getId(), id));
    }

    // Devuelve el archivo, no JSON: el front lo baja como orden-de-compra-<id>.pdf
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UsuarioPrincipal principal,
                                      @PathVariable Long id) {
        String archivo = "orden-de-compra-" + id + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(archivo).build().toString())
                .body(ordenService.ordenDeCompraPdf(principal.getId(), id));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<OrdenResponse> cancelar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(ordenService.cancelar(principal.getId(), id));
    }
}
