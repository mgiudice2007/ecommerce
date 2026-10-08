package com.ecommerce.vuelos.dto.carrito;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body opcional del checkout: cuantas millas quiere usar el comprador para pagar. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @Min(0)
    private Integer millas;
}
