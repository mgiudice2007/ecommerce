package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.embarque.EmbarqueResponse;

public interface EmbarqueService {

    /** Busca el pasaje por su codigo y dice si se puede embarcar. 404 si el codigo no existe. */
    EmbarqueResponse validar(String codigoEmbarque);

    /** Marca al pasajero como embarcado. 400 si ya embarco o si la compra o el vuelo estan cancelados. */
    EmbarqueResponse embarcar(String codigoEmbarque);
}
