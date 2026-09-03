package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.carrito.CarritoResponse;
import com.ecommerce.vuelos.dto.carrito.ItemCarritoRequest;
import com.ecommerce.vuelos.dto.carrito.ItemCarritoResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.entity.*;
import com.ecommerce.vuelos.repository.CarritoRepository;
import com.ecommerce.vuelos.repository.OrdenRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final VueloRepository vueloRepository;
    private final OrdenRepository ordenRepository;
    private final OrdenService ordenService;

    public CarritoResponse obtenerCarrito(Long pasajeroId) {
        return toResponse(buscarCarrito(pasajeroId));
    }

    @Transactional
    public CarritoResponse agregarItem(Long pasajeroId, ItemCarritoRequest request) {
        Carrito carrito = buscarCarrito(pasajeroId);
        Vuelo vuelo = vueloRepository.findById(request.getVueloId())
                .orElseThrow(() -> new ResourceNotFoundException("Vuelo no encontrado: " + request.getVueloId()));

        if (request.getCantidad() > vuelo.getAsientosDisponibles()) {
            throw new BadRequestException("No hay suficientes asientos disponibles para el vuelo " + vuelo.getId());
        }

        ItemCarrito itemExistente = carrito.getItems().stream()
                .filter(item -> item.getVuelo().getId().equals(vuelo.getId()))
                .findFirst()
                .orElse(null);

        if (itemExistente != null) {
            int nuevaCantidad = itemExistente.getCantidad() + request.getCantidad();
            if (nuevaCantidad > vuelo.getAsientosDisponibles()) {
                throw new BadRequestException("No hay suficientes asientos disponibles para el vuelo " + vuelo.getId());
            }
            itemExistente.setCantidad(nuevaCantidad);
        } else {
            ItemCarrito nuevoItem = ItemCarrito.builder()
                    .carrito(carrito)
                    .vuelo(vuelo)
                    .cantidad(request.getCantidad())
                    .build();
            carrito.getItems().add(nuevoItem);
        }

        carritoRepository.save(carrito);
        return toResponse(carrito);
    }

    @Transactional
    public CarritoResponse actualizarItem(Long pasajeroId, Long itemId, Integer cantidad) {
        Carrito carrito = buscarCarrito(pasajeroId);
        ItemCarrito item = obtenerItemDelCarrito(carrito, itemId);

        if (cantidad > item.getVuelo().getAsientosDisponibles()) {
            throw new BadRequestException("No hay suficientes asientos disponibles para el vuelo " + item.getVuelo().getId());
        }

        item.setCantidad(cantidad);
        carritoRepository.save(carrito);
        return toResponse(carrito);
    }

    @Transactional
    public CarritoResponse eliminarItem(Long pasajeroId, Long itemId) {
        Carrito carrito = buscarCarrito(pasajeroId);
        ItemCarrito item = obtenerItemDelCarrito(carrito, itemId);

        carrito.getItems().remove(item);
        carritoRepository.save(carrito);
        return toResponse(carrito);
    }

    @Transactional
    public OrdenResponse checkout(Long pasajeroId) {
        Carrito carrito = buscarCarrito(pasajeroId);

        if (carrito.getItems().isEmpty()) {
            throw new BadRequestException("El carrito esta vacio");
        }

        // Validar stock de todos los items antes de descontar nada
        for (ItemCarrito item : carrito.getItems()) {
            Vuelo vuelo = item.getVuelo();
            if (item.getCantidad() > vuelo.getAsientosDisponibles()) {
                throw new BadRequestException("No hay suficientes asientos disponibles para el vuelo "
                        + vuelo.getOrigen() + " - " + vuelo.getDestino());
            }
        }

        List<ItemOrden> itemsOrden = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ItemCarrito item : carrito.getItems()) {
            Vuelo vuelo = item.getVuelo();
            BigDecimal precioUnitario = vuelo.getPrecioConDescuento();

            vuelo.setAsientosDisponibles(vuelo.getAsientosDisponibles() - item.getCantidad());
            vueloRepository.save(vuelo);

            ItemOrden itemOrden = ItemOrden.builder()
                    .vuelo(vuelo)
                    .cantidad(item.getCantidad())
                    .precioUnitario(precioUnitario)
                    .build();
            itemsOrden.add(itemOrden);

            total = total.add(precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        Orden orden = Orden.builder()
                .pasajero(carrito.getPasajero())
                .total(total)
                .fecha(LocalDateTime.now())
                .estado(EstadoOrden.CONFIRMADA)
                .items(new ArrayList<>())
                .build();

        itemsOrden.forEach(ir -> {
            ir.setOrden(orden);
            orden.getItems().add(ir);
        });

        Orden guardada = ordenRepository.save(orden);

        carrito.getItems().clear();
        carritoRepository.save(carrito);

        return ordenService.toResponse(guardada);
    }

    private ItemCarrito obtenerItemDelCarrito(Carrito carrito, Long itemId) {
        return carrito.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado en el carrito: " + itemId));
    }

    private Carrito buscarCarrito(Long pasajeroId) {
        return carritoRepository.findByPasajeroId(pasajeroId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado para el pasajero: " + pasajeroId));
    }

    private CarritoResponse toResponse(Carrito carrito) {
        List<ItemCarritoResponse> items = carrito.getItems().stream()
                .map(item -> ItemCarritoResponse.builder()
                        .id(item.getId())
                        .vueloId(item.getVuelo().getId())
                        .origen(item.getVuelo().getOrigen())
                        .destino(item.getVuelo().getDestino())
                        .precioUnitario(item.getVuelo().getPrecioConDescuento())
                        .cantidad(item.getCantidad())
                        .subtotal(item.getVuelo().getPrecioConDescuento().multiply(BigDecimal.valueOf(item.getCantidad())))
                        .build())
                .toList();

        BigDecimal total = items.stream()
                .map(ItemCarritoResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CarritoResponse.builder()
                .id(carrito.getId())
                .items(items)
                .total(total)
                .build();
    }
}
