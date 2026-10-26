package com.ecommerce.vuelos.dto.usuario;

import com.ecommerce.vuelos.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
public class UsuarioAdminResponse {

    private Long id;
    private String username;
    private String mail;
    private String nombre;
    private String apellido;
    private String rol;
    private LocalDateTime fechaRegistro;

    public static UsuarioAdminResponse desde(Usuario usuario) {
        return UsuarioAdminResponse.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .mail(usuario.getMail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .rol(usuario.getRol().name())
                .fechaRegistro(usuario.getFechaRegistro())
                .build();
    }
}
