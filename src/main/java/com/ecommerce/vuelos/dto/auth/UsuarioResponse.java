package com.ecommerce.vuelos.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String username;
    private String mail;
    private String nombre;
    private String apellido;
    private String rol;
}
