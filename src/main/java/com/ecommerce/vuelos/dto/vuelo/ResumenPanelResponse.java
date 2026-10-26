package com.ecommerce.vuelos.dto.vuelo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Los numeros de arriba del panel del admin, calculados en la base (sin traer todos los vuelos). */
@Getter
@AllArgsConstructor
public class ResumenPanelResponse {
    private long vuelos;
    private long asientosVendidos;
    private long asientosTotales;
}
