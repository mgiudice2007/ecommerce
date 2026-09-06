package com.ecommerce.vuelos.dto.catalogo;

import com.ecommerce.vuelos.entity.Aeropuerto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AeropuertoResponse {

    private String codigoIata;
    private String nombre;
    private String ciudad;
    private String provincia;
    private String pais;

    public static AeropuertoResponse desde(Aeropuerto aeropuerto) {
        return AeropuertoResponse.builder()
                .codigoIata(aeropuerto.getCodigoIata())
                .nombre(aeropuerto.getNombre())
                .ciudad(aeropuerto.getCiudad())
                .provincia(aeropuerto.getProvincia())
                .pais(aeropuerto.getPais())
                .build();
    }
}
