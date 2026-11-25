package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.carrito.PasajeroRequest;
import com.ecommerce.vuelos.entity.Carrito;
import com.ecommerce.vuelos.entity.ItemCarrito;
import com.ecommerce.vuelos.entity.ItemOrden;
import com.ecommerce.vuelos.entity.Pasaje;

import java.util.List;
import java.util.Map;

public interface PasajeService {

    /**
     * Revisa los datos de los pasajeros contra el carrito y los agrupa por pasaje del carrito.
     * Tiene que haber una persona por cada lugar comprado (bebes incluidos), con una edad que
     * corresponda a su tipo de pasajero en la fecha del vuelo, y nadie repetido en un mismo vuelo.
     */
    Map<Long, List<PasajeroRequest>> validar(Carrito carrito, List<PasajeroRequest> pasajeros);

    /** Arma los pasajes de un item de la orden: cada persona con su asiento y su codigo de embarque. */
    List<Pasaje> crearPasajes(ItemOrden itemOrden, ItemCarrito item, List<PasajeroRequest> pasajeros);
}
