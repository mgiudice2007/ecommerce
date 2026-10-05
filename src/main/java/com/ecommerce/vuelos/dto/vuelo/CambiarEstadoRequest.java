package com.ecommerce.vuelos.dto.vuelo;

import com.ecommerce.vuelos.entity.EstadoVuelo;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cuerpo del PATCH de estado: solo el estado nuevo, nada de la descripcion ni del resto del vuelo. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambiarEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoVuelo estado;
}
