package com.ecommerce.vuelos.dto.aerolinea;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AerolineaRequest {

    @NotBlank
    private String nombre;
}
