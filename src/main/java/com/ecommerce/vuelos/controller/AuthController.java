package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.auth.LoginRequest;
import com.ecommerce.vuelos.dto.auth.RegisterAdministradorRequest;
import com.ecommerce.vuelos.dto.auth.RegisterPasajeroRequest;
import com.ecommerce.vuelos.dto.auth.UsuarioResponse;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/registro/pasajero")
    public ResponseEntity<UsuarioResponse> registrarPasajero(@Valid @RequestBody RegisterPasajeroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarPasajero(request));
    }

    @PostMapping("/registro/administrador")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponse> registrarAdministrador(@Valid @RequestBody RegisterAdministradorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarAdministrador(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(@Valid @RequestBody LoginRequest request,
                                                  HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        String rol = principal.isAdministrador() ? "ADMINISTRADOR" : "PASAJERO";
        return ResponseEntity.ok(AuthService.toResponse(principal.getUsuario(), rol));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        String rol = principal.isAdministrador() ? "ADMINISTRADOR" : "PASAJERO";
        return ResponseEntity.ok(AuthService.toResponse(principal.getUsuario(), rol));
    }
}
