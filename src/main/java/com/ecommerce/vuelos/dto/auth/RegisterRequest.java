package com.ecommerce.vuelos.dto.auth;

import com.ecommerce.vuelos.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    // Usuario: de 4 a 20 caracteres, empieza con letra, sin espacios ni tildes
    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 4, max = 20, message = "El usuario tiene que tener entre 4 y 20 caracteres")
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9._]*",
            message = "El usuario tiene que empezar con una letra y solo puede tener letras sin tilde, números, punto o guion bajo")
    private String username;

    @NotBlank
    @Email
    private String mail;

    @NotBlank
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    // Nombre y apellido: solo letras (con tildes y ñ), espacios, apostrofo o guion
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

    /**
     * Opcional. El registro publico crea siempre un COMPRADOR (hay un unico
     * vendedor, el de la aerolinea); si llega otro rol se rechaza.
     */
    private Rol rol;
}
