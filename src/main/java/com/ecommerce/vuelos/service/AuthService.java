package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.auth.RegisterRequest;
import com.ecommerce.vuelos.dto.auth.UsuarioResponse;
import com.ecommerce.vuelos.entity.Carrito;
import com.ecommerce.vuelos.entity.Rol;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Registro publico. Solo permite COMPRADOR o VENDEDOR. */
    @Transactional
    public UsuarioResponse registrar(RegisterRequest request) {
        if (request.getRol() == Rol.ADMIN) {
            throw new BadRequestException("No es posible registrarse como ADMIN");
        }
        return crear(request, request.getRol());
    }

    /** Alta de administrador. Solo la puede invocar un ADMIN autenticado. */
    @Transactional
    public UsuarioResponse registrarAdministrador(RegisterRequest request) {
        return crear(request, Rol.ADMIN);
    }

    private UsuarioResponse crear(RegisterRequest request, Rol rol) {
        validarDisponibilidad(request.getUsername(), request.getMail());

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .mail(request.getMail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .rol(rol)
                .build();

        // El comprador es el unico que necesita carrito.
        if (rol == Rol.COMPRADOR) {
            usuario.setCarrito(Carrito.builder().usuario(usuario).build());
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    private void validarDisponibilidad(String username, String mail) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new BadRequestException("El nombre de usuario ya esta en uso");
        }
        if (usuarioRepository.existsByMail(mail)) {
            throw new BadRequestException("El mail ya esta registrado");
        }
    }

    public static UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .mail(usuario.getMail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .rol(usuario.getRol().name())
                .build();
    }
}
