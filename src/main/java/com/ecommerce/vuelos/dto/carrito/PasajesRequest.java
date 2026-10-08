package com.ecommerce.vuelos.dto.carrito;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Los pasajeros que se agregan juntos al carrito para un vuelo y clase (la disponibilidad). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasajesRequest {

    @NotNull
    private Long disponibilidadId;

    @NotNull
    @Min(0)
    private Integer adultos;

    @NotNull
    @Min(0)
    private Integer ninos;

    @NotNull
    @Min(0)
    private Integer bebes;
}
