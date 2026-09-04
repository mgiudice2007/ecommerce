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

    /** Datos del pasajero. Pueden venir en null hasta que el usuario los complete. */
    private String dni;
    private LocalDate fechaNacimiento;
    private String telefono;

    private String rol;
    private LocalDateTime fechaRegistro;

    /**
     * Arma la respuesta a partir de la entidad. Notar que la password nunca se
     * copia: es justamente el motivo por el que devolvemos un DTO y no el Usuario.
     */
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
