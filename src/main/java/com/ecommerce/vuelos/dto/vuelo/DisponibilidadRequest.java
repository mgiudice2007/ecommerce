package com.ecommerce.vuelos.dto.vuelo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadRequest {

    @NotNull
    private Long vueloId;

    @NotNull
    private Long claseId;

    
    @NotNull(message = "La cantidad de asientos es obligatoria")
    @Min(value = 1, message = "La clase tiene que tener al menos 1 asiento")
    private Integer asientosTotales;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio tiene que ser mayor a cero")
    private BigDecimal precio;
}
