package com.ecommerce.vuelos.dto.carrito;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Los asientos que eligio el comprador para los pasajeros de un vuelo y clase del carrito. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsientosRequest {

    @NotNull
    private Long disponibilidadId;

    @NotNull
    private List<String> asientos;
}
