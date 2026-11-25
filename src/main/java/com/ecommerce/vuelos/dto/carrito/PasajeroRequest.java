package com.ecommerce.vuelos.dto.carrito;

import com.ecommerce.vuelos.entity.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Los datos de una persona que viaja: se piden antes de pagar, como en una aerolinea. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasajeroRequest {

    @NotNull(message = "Falta indicar a qué pasaje del carrito corresponde el pasajero")
    private Long itemId;

    @NotBlank(message = "El nombre del pasajero es obligatorio")
    @Size(min = 2, max = 40, message = "El nombre tiene que tener entre 2 y 40 letras")
    @Pattern(regexp = "\\p{L}[\\p{L}' -]*",
            message = "El nombre solo puede tener letras, espacios, apóstrofo o guion")
    private String nombre;

    @NotBlank(message = "El apellido del pasajero es obligatorio")
    @Size(min = 2, max = 40, message = "El apellido tiene que tener entre 2 y 40 letras")
    @Pattern(regexp = "\\p{L}[\\p{L}' -]*",
            message = "El apellido solo puede tener letras, espacios, apóstrofo o guion")
    private String apellido;

    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El documento es obligatorio")
    @Pattern(regexp = "[A-Za-z0-9]{6,12}", message = "El documento tiene que tener entre 6 y 12 letras o números, sin puntos")
    private String documento;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento tiene que ser pasada")
    private LocalDate fechaNacimiento;
}
