package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.auth.RegisterAdministradorRequest;
import com.ecommerce.vuelos.dto.auth.RegisterPasajeroRequest;
import com.ecommerce.vuelos.dto.auth.UsuarioResponse;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.entity.Administrador;
import com.ecommerce.vuelos.entity.Carrito;
import com.ecommerce.vuelos.entity.Pasajero;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.repository.AdministradorRepository;
import com.ecommerce.vuelos.repository.PasajeroRepository;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasajeroRepository pasajeroRepository;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse registrarPasajero(RegisterPasajeroRequest request) {
        validarDisponibilidad(request.getUsername(), request.getMail());

        Pasajero pasajero = Pasajero.builder()
                .username(request.getUsername())
                .mail(request.getMail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .build();

        Carrito carrito = Carrito.builder()
                .pasajero(pasajero)
                .build();
        pasajero.setCarrito(carrito);

        Pasajero guardado = pasajeroRepository.save(pasajero);
        return toResponse(guardado, "PASAJERO");
    }

    @Transactional
    public UsuarioResponse registrarAdministrador(RegisterAdministradorRequest request) {
        validarDisponibilidad(request.getUsername(), request.getMail());

        Administrador administrador = Administrador.builder()
                .username(request.getUsername())
                .mail(request.getMail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .permisos(request.getPermisos() != null ? request.getPermisos() : new HashSet<>())
                .build();

        Administrador guardado = administradorRepository.save(administrador);
        return toResponse(guardado, "ADMINISTRADOR");
    }

    private void validarDisponibilidad(String username, String mail) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new BadRequestException("El nombre de usuario ya esta en uso");
        }
        if (usuarioRepository.existsByMail(mail)) {
            throw new BadRequestException("El mail ya esta registrado");
        }
    }

    public static UsuarioResponse toResponse(Usuario usuario, String rol) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .mail(usuario.getMail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .rol(rol)
                .build();
    }
}
