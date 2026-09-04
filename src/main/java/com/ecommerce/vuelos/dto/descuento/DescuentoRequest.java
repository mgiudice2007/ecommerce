package com.ecommerce.vuelos.dto.descuento;

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

    @NotNull(message = "Falta el vueloId")
    private Long vueloId;

    @NotNull(message = "El tipo es obligatorio: PORCENTAJE o MONTO_FIJO")
    private TipoDescuento tipoDescuento;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "El valor tiene que ser mayor a cero")
    private BigDecimal valor;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaDesde;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaHasta;

    /** Si no viene, el descuento nace prendido. */
    private Boolean activo;
}
