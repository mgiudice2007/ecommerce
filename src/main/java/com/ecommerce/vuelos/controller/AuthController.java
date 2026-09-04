package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.auth.ActualizarPerfilRequest;
import com.ecommerce.vuelos.dto.auth.LoginRequest;
import com.ecommerce.vuelos.dto.auth.LoginResponse;
import com.ecommerce.vuelos.dto.auth.RegisterRequest;
import com.ecommerce.vuelos.dto.auth.UsuarioResponse;
import com.ecommerce.vuelos.security.JwtService;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/registro/administrador")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> registrarAdministrador(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarAdministrador(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        return ResponseEntity.ok(new LoginResponse(token, UsuarioResponse.desde(principal.getUsuario())));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(UsuarioResponse.desde(principal.getUsuario()));
    }

    /**
     * El usuario completa o corrige sus propios datos, incluidos los de
     * pasajero. No cambia username, mail, password ni rol.
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<UsuarioResponse> actualizarPerfil(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return ResponseEntity.ok(authService.actualizarPerfil(principal.getId(), request));
    }
}
