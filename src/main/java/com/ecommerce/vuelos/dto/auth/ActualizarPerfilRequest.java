package com.ecommerce.vuelos.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPerfilRequest {

 
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 40, message = "El nombre tiene que tener entre 2 y 40 letras")
    @Pattern(regexp = "\\p{L}[\\p{L}' -]*",
            message = "El nombre solo puede tener letras, espacios, apóstrofo o guion")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 40, message = "El apellido tiene que tener entre 2 y 40 letras")
    @Pattern(regexp = "\\p{L}[\\p{L}' -]*",
            message = "El apellido solo puede tener letras, espacios, apóstrofo o guion")
    private String apellido;

    
    @Pattern(regexp = "\\d{7,9}|", message = "El DNI tiene que ser de 7 a 9 digitos")
    private String dni;

    @Past(message = "La fecha de nacimiento tiene que ser pasada")
    private LocalDate fechaNacimiento;

    private String telefono;
}
