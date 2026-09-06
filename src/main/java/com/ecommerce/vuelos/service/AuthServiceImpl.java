package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.auth.ActualizarPerfilRequest;
import com.ecommerce.vuelos.dto.auth.RegisterRequest;
import com.ecommerce.vuelos.dto.auth.UsuarioResponse;
import com.ecommerce.vuelos.entity.Carrito;
import com.ecommerce.vuelos.entity.Rol;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Registro publico. Solo permite COMPRADOR o VENDEDOR. */
    @Override
    @Transactional
    public UsuarioResponse registrar(RegisterRequest request) {
        if (request.getRol() == Rol.ADMIN) {
            throw new BadRequestException("No es posible registrarse como ADMIN");
        }
        return crear(request, request.getRol());
    }

    /** Alta de administrador. Solo la puede invocar un ADMIN autenticado. */
    @Override
    @Transactional
    public UsuarioResponse registrarAdministrador(RegisterRequest request) {
        return crear(request, Rol.ADMIN);
    }

    @Override
    @Transactional
    public UsuarioResponse actualizarPerfil(Long usuarioId, ActualizarPerfilRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

        String dni = normalizar(request.getDni());
        // El DNI identifica a una persona: no puede repetirse entre cuentas.
        if (dni != null && !dni.equals(usuario.getDni())
                && usuarioRepository.existsByDni(dni)) {
            throw new BadRequestException("Ese DNI ya esta registrado en otra cuenta");
        }

        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setDni(dni);
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setTelefono(normalizar(request.getTelefono()));

        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    /** Un string vacio se guarda como null, para que el UNIQUE del DNI no choque. */
    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
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
                .fechaRegistro(LocalDateTime.now())
                .build();

        // El comprador es el unico que necesita carrito.
        if (rol == Rol.COMPRADOR) {
            usuario.setCarrito(Carrito.builder().usuario(usuario).build());
        }

        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    private void validarDisponibilidad(String username, String mail) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new BadRequestException("El nombre de usuario ya esta en uso");
        }
        if (usuarioRepository.existsByMail(mail)) {
            throw new BadRequestException("El mail ya esta registrado");
        }
    }
}
