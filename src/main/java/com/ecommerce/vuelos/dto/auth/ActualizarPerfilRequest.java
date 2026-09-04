package com.ecommerce.vuelos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Los datos que el usuario puede cambiar de si mismo. No incluye username,
 * mail, password ni rol: eso no se toca por aca.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPerfilRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    /** Los tres siguientes son los datos del pasajero, y pueden venir vacios. */
    @Pattern(regexp = "\\d{7,9}|", message = "El DNI tiene que ser de 7 a 9 digitos")
    private String dni;

    @Past(message = "La fecha de nacimiento tiene que ser pasada")
    private LocalDate fechaNacimiento;

    private String telefono;
}
