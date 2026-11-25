package com.ecommerce.vuelos.dto.orden;

import com.ecommerce.vuelos.entity.Pasaje;
import com.ecommerce.vuelos.entity.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PasajeResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private TipoDocumento tipoDocumento;
    private String documento;
    private LocalDate fechaNacimiento;
    private String asiento;
    private String codigoEmbarque;
    private LocalDateTime embarcadoEn;

    public static PasajeResponse desde(Pasaje pasaje) {
        return PasajeResponse.builder()
                .id(pasaje.getId())
                .nombre(pasaje.getNombre())
                .apellido(pasaje.getApellido())
                .tipoDocumento(pasaje.getTipoDocumento())
                .documento(pasaje.getDocumento())
                .fechaNacimiento(pasaje.getFechaNacimiento())
                .asiento(pasaje.getAsiento())
                .codigoEmbarque(pasaje.getCodigoEmbarque())
                .embarcadoEn(pasaje.getEmbarcadoEn())
                .build();
    }
}
