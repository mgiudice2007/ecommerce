package com.ecommerce.vuelos.dto.vuelo;

import com.ecommerce.vuelos.entity.TipoDescuento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescuentoRequest {

    @NotNull
    private Long vueloId;

    @NotNull
    private TipoDescuento tipoDescuento;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal valor;

    @NotNull
    private LocalDate fechaDesde;

    @NotNull
    private LocalDate fechaHasta;

    @NotNull
    private Boolean activo;
}
