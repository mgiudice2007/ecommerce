package com.ecommerce.vuelos.dto.auth;

import com.ecommerce.vuelos.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String mail;

    @NotBlank
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotNull(message = "El rol es obligatorio: COMPRADOR o VENDEDOR")
    private Rol rol;
}
