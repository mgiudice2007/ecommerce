package com.ecommerce.vuelos.dto.auth;

import com.ecommerce.vuelos.entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String username;
    private String mail;
    private String nombre;
    private String apellido;

    private String dni;
    private LocalDate fechaNacimiento;
    private String telefono;

    private String rol;
    private LocalDateTime fechaRegistro;


    public static UsuarioResponse desde(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .mail(usuario.getMail())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .dni(usuario.getDni())
                .fechaNacimiento(usuario.getFechaNacimiento())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol().name())
                .fechaRegistro(usuario.getFechaRegistro())
                .build();
    }
}
