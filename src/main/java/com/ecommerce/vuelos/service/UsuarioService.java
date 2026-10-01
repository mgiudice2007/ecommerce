package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.usuario.CambiarRolRequest;
import com.ecommerce.vuelos.dto.usuario.UsuarioAdminResponse;

import java.util.List;

/**
 * "Administracion de cuentas de usuario, incluyendo la asignacion de permisos"
 * del enunciado: el ADMIN ve todas las cuentas y les cambia el rol.
 */
public interface UsuarioService {

    List<UsuarioAdminResponse> listar();

    UsuarioAdminResponse cambiarRol(Long usuarioId, CambiarRolRequest request, Long adminId);
}
