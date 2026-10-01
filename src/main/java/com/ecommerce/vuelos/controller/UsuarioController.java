package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.usuario.CambiarRolRequest;
import com.ecommerce.vuelos.dto.usuario.UsuarioAdminResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioAdminResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<UsuarioAdminResponse> cambiarRol(@PathVariable Long id,
                                                           @Valid @RequestBody CambiarRolRequest request,
                                                           @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, request, principal.getId()));
    }
}
