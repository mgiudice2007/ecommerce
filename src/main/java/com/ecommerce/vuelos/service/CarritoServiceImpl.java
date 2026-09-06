package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.carrito.CarritoResponse;
import com.ecommerce.vuelos.dto.carrito.ItemCarritoRequest;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.entity.Carrito;
import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.EstadoVuelo;
import com.ecommerce.vuelos.entity.ItemCarrito;
import com.ecommerce.vuelos.entity.ItemOrden;
import com.ecommerce.vuelos.entity.Orden;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.CarritoRepository;
import com.ecommerce.vuelos.repository.DisponibilidadRepository;
import com.ecommerce.vuelos.repository.OrdenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CarritoServiceImpl implements CarritoService {

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private DisponibilidadRepository disponibilidadRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    @Override
    public CarritoResponse obtenerCarrito(Long usuarioId) {
        return CarritoResponse.desde(buscarCarrito(usuarioId));
    }

    @Override
    @Transactional
    public CarritoResponse agregarItem(Long usuarioId, ItemCarritoRequest request) {
        Carrito carrito = buscarCarrito(usuarioId);
        Disponibilidad disponibilidad = disponibilidadRepository.findById(request.getDisponibilidadId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Disponibilidad no encontrada: " + request.getDisponibilidadId()));

        validarPublicado(disponibilidad.getVuelo());
        validarStock(disponibilidad, request.getCantidad());

        ItemCarrito itemExistente = carrito.getItems().stream()
                .filter(item -> item.getDisponibilidad().getId().equals(disponibilidad.getId()))
                .findFirst()
                .orElse(null);

        if (itemExistente != null) {
            validarStock(disponibilidad, itemExistente.getCantidad() + request.getCantidad());
            itemExistente.setCantidad(itemExistente.getCantidad() + request.getCantidad());
        } else {
            carrito.getItems().add(ItemCarrito.builder()
                    .carrito(carrito)
                    .disponibilidad(disponibilidad)
                    .cantidad(request.getCantidad())
                    .build());
        }

        carritoRepository.save(carrito);
        return CarritoResponse.desde(carrito);
    }

    @Override
    @Transactional
    public CarritoResponse actualizarItem(Long usuarioId, Long itemId, Integer cantidad) {
        Carrito carrito = buscarCarrito(usuarioId);
        ItemCarrito item = obtenerItemDelCarrito(carrito, itemId);

        validarStock(item.getDisponibilidad(), cantidad);
        item.setCantidad(cantidad);
        carritoRepository.save(carrito);
        return CarritoResponse.desde(carrito);
    }

    @Override
    @Transactional
    public CarritoResponse eliminarItem(Long usuarioId, Long itemId) {
        Carrito carrito = buscarCarrito(usuarioId);
        ItemCarrito item = obtenerItemDelCarrito(carrito, itemId);

        carrito.getItems().remove(item);
        carritoRepository.save(carrito);
        return CarritoResponse.desde(carrito);
    }

    /**
     * La operacion transaccional: descuenta stock, crea la orden y vacia el
     * carrito. Si algo falla en el medio, @Transactional revierte los tres
     * pasos y no queda una orden a medio hacer.
     */
    @Override
    @Transactional
    public OrdenResponse checkout(Long usuarioId) {
        Carrito carrito = buscarCarrito(usuarioId);

        if (carrito.getItems().isEmpty()) {
            throw new BadRequestException("El carrito esta vacio");
        }

        // Validar el stock de todos los items antes de descontar nada.
        for (ItemCarrito item : carrito.getItems()) {
            validarPublicado(item.getDisponibilidad().getVuelo());
            validarStock(item.getDisponibilidad(), item.getCantidad());
        }

        List<ItemOrden> itemsOrden = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;

        for (ItemCarrito item : carrito.getItems()) {
            Disponibilidad disponibilidad = item.getDisponibilidad();
            BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
            BigDecimal precioUnitario = disponibilidad.getPrecioConDescuento();
            BigDecimal descuentoUnitario = disponibilidad.getDescuentoUnitario();

            disponibilidad.setAsientosDisponibles(
                    disponibilidad.getAsientosDisponibles() - item.getCantidad());
            disponibilidadRepository.save(disponibilidad);

            // El precio y el descuento se congelan aca: si manana el vendedor
            // cambia cualquiera de los dos, la orden ya emitida sigue valiendo
            // lo que el comprador pago y muestra lo que se ahorro ese dia.
            itemsOrden.add(ItemOrden.builder()
                    .disponibilidad(disponibilidad)
                    .cantidad(item.getCantidad())
                    .precioUnitario(precioUnitario)
                    .descuentoAplicado(descuentoUnitario)
                    .build());

            total = total.add(precioUnitario.multiply(cantidad));
            descuentoTotal = descuentoTotal.add(descuentoUnitario.multiply(cantidad));
        }

        Orden orden = Orden.builder()
                .usuario(carrito.getUsuario())
                .total(total)
                .descuentoTotal(descuentoTotal)
                .fecha(LocalDateTime.now())
                .estado(EstadoOrden.CONFIRMADA)
                .items(new ArrayList<>())
                .build();

        itemsOrden.forEach(itemOrden -> {
            itemOrden.setOrden(orden);
            orden.getItems().add(itemOrden);
        });

        Orden guardada = ordenRepository.save(orden);

        carrito.getItems().clear();
        carritoRepository.save(carrito);

        return OrdenResponse.desde(guardada);
    }

    private void validarPublicado(Vuelo vuelo) {
        if (vuelo.getEstado() != EstadoVuelo.ACTIVO) {
            throw new BadRequestException("El vuelo " + vuelo.getNumeroVuelo() + " ya no esta disponible");
        }
    }

    private void validarStock(Disponibilidad disponibilidad, int cantidad) {
        if (cantidad > disponibilidad.getAsientosDisponibles()) {
            throw new BadRequestException("No hay suficientes asientos en clase "
                    + disponibilidad.getClase().getNombre() + " para el vuelo "
                    + disponibilidad.getVuelo().getNumeroVuelo());
        }
    }

    private ItemCarrito obtenerItemDelCarrito(Carrito carrito, Long itemId) {
        return carrito.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado en el carrito: " + itemId));
    }

    private Carrito buscarCarrito(Long usuarioId) {
        return carritoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carrito no encontrado para el usuario: " + usuarioId));
    }
}
