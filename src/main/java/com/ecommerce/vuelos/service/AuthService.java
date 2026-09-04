package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.auth.ActualizarPerfilRequest;
import com.ecommerce.vuelos.dto.auth.RegisterRequest;
import com.ecommerce.vuelos.dto.auth.UsuarioResponse;

/**
 * Contrato del alta de usuarios. El controller depende de esta interfaz y no de
 * la implementacion: eso es lo que permite que Spring inyecte la clase concreta
 * y que manana se pueda cambiar sin tocar el controller.
 */
public interface AuthService {

    UsuarioResponse registrar(RegisterRequest request);

    UsuarioResponse registrarAdministrador(RegisterRequest request);

    /**
     * "Administracion de cuentas de usuario" del enunciado: el usuario completa
     * o corrige sus datos, incluidos los de pasajero, despues del registro.
     */
    UsuarioResponse actualizarPerfil(Long usuarioId, ActualizarPerfilRequest request);
}
