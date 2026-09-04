package com.ecommerce.vuelos.dto.vuelo;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VueloRequest {

    @NotBlank
    private String numeroVuelo;

    private String descripcion;

    @NotNull
    private Long categoriaId;

    @NotBlank
    private String origenIata;

    @NotBlank
    private String destinoIata;

    @NotNull
    @Future(message = "La fecha de salida debe ser futura")
    private LocalDateTime fechaSalida;

    @NotNull
    @Future(message = "La fecha de llegada debe ser futura")
    private LocalDateTime fechaLlegada;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precio;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal descuento;
}
