package com.ecommerce.vuelos.dto.carrito;

import com.ecommerce.vuelos.entity.MedioPago;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @Min(0)
    private Integer millas;

    private MedioPago medioPago;

    @Pattern(regexp = "\\d{4}", message = "Los últimos dígitos son 4 números")
    private String ultimosDigitos;
}
