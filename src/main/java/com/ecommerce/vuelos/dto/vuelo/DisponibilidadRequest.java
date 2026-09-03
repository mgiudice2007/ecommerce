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

    @NotNull
    @Min(0)
    private Integer asientosTotales;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precio;
}
