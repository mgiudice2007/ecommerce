package com.ecommerce.vuelos.dto.carrito;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCarritoRequest {

    @NotNull
    private Long disponibilidadId;

    @NotNull
    @Min(1)
    private Integer cantidad;
}
