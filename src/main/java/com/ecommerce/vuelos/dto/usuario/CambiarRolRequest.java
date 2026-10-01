package com.ecommerce.vuelos.dto.usuario;

import com.ecommerce.vuelos.entity.Rol;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambiarRolRequest {

    @NotNull(message = "El rol es obligatorio: COMPRADOR, VENDEDOR o ADMIN")
    private Rol rol;
}
