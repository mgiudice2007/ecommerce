package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.carrito.CarritoResponse;
import com.ecommerce.vuelos.dto.carrito.ItemCarritoRequest;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;

public interface CarritoService {

    CarritoResponse obtenerCarrito(Long usuarioId);

    CarritoResponse agregarItem(Long usuarioId, ItemCarritoRequest request);

    CarritoResponse actualizarItem(Long usuarioId, Long itemId, Integer cantidad);

    CarritoResponse eliminarItem(Long usuarioId, Long itemId);

    OrdenResponse checkout(Long usuarioId);
}
