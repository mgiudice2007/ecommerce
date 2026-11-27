package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.OrdenResponse;

import java.util.List;

public interface OrdenService {

    List<OrdenResponse> historial(Long usuarioId);

    OrdenResponse obtener(Long usuarioId, Long ordenId);

    OrdenResponse cancelar(Long usuarioId, Long ordenId);

    /** La orden de compra en PDF, la misma que viaja adjunta en el mail de confirmacion. */
    byte[] ordenDeCompraPdf(Long usuarioId, Long ordenId);
}
