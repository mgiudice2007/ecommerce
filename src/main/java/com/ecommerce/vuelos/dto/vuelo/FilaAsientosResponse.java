package com.ecommerce.vuelos.dto.vuelo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


@Getter
@AllArgsConstructor
public class FilaAsientosResponse {

    private Integer numero;
    private List<String> asientos;
}
