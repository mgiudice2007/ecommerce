package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.carrito.CarritoResponse;
import com.ecommerce.vuelos.dto.carrito.ItemCarritoRequest;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.entity.Carrito;
import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.ItemCarrito;
import com.ecommerce.vuelos.entity.ItemOrden;
import com.ecommerce.vuelos.entity.Orden;
import com.ecommerce.vuelos.entity.TipoPasajero;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.CarritoRepository;
import com.ecommerce.vuelos.repository.DisponibilidadRepository;
import com.ecommerce.vuelos.repository.OrdenRepository;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Reglas de las millas, configurables en application.properties (no estan fijas en el codigo)
    @Value("${millas.pesos-por-milla}")
    private BigDecimal pesosPorMilla;
    @Value("${millas.valor-en-pesos}")
    private BigDecimal valorMilla;

    @Override
    @Transactional
    public CarritoResponse obtenerCarrito(Long usuarioId) {
        return respuesta(buscarCarrito(usuarioId));
    }

    @Override
    @Transactional
    public CarritoResponse agregarItem(Long usuarioId, ItemCarritoRequest request) {
        Carrito carrito = buscarCarrito(usuarioId);
        Disponibilidad disponibilidad = disponibilidadRepository.findById(request.getDisponibilidadId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Disponibilidad no encontrada: " + request.getDisponibilidadId()));

        TipoPasajero tipo = request.getTipoPasajero() == null ? TipoPasajero.ADULTO : request.getTipoPasajero();

        validarPublicado(disponibilidad.getVuelo());
        // Adultos, ninos y bebes del mismo vuelo y clase ocupan los mismos asientos
        validarStock(disponibilidad, asientosEnCarrito(carrito, disponibilidad) + request.getCantidad());

        // Si ya habia pasajeros de ese tipo en ese vuelo y clase, se suman
        ItemCarrito itemExistente = carrito.getItems().stream()
                .filter(item -> item.getDisponibilidad().getId().equals(disponibilidad.getId()))
                .filter(item -> item.getTipoPasajero() == tipo)
                .findFirst()
                .orElse(null);

        if (itemExistente != null) {
            itemExistente.setCantidad(itemExistente.getCantidad() + request.getCantidad());
        } else {
            carrito.getItems().add(ItemCarrito.builder()
                    .carrito(carrito)
                    .disponibilidad(disponibilidad)
                    .cantidad(request.getCantidad())
                    .tipoPasajero(tipo)
                    .build());
        }

        carritoRepository.save(carrito);
        return respuesta(carrito);
    }

    @Override
    @Transactional
    public CarritoResponse actualizarItem(Long usuarioId, Long itemId, Integer cantidad) {
        Carrito carrito = buscarCarrito(usuarioId);
        ItemCarrito item = obtenerItemDelCarrito(carrito, itemId);

        int otrosPasajeros = asientosEnCarrito(carrito, item.getDisponibilidad()) - item.getCantidad();
        validarStock(item.getDisponibilidad(), otrosPasajeros + cantidad);
        item.setCantidad(cantidad);
        carritoRepository.save(carrito);
        return respuesta(carrito);
    }

    @Override
    @Transactional
    public CarritoResponse eliminarItem(Long usuarioId, Long itemId) {
        Carrito carrito = buscarCarrito(usuarioId);
        ItemCarrito item = obtenerItemDelCarrito(carrito, itemId);

        carrito.getItems().remove(item);
        carritoRepository.save(carrito);
        return respuesta(carrito);
    }

    @Override
    @Transactional
    public OrdenResponse checkout(Long usuarioId, Integer millasAUsar) {
        Carrito carrito = buscarCarrito(usuarioId);

        if (carrito.getItems().isEmpty()) {
            throw new BadRequestException("El carrito esta vacio");
        }

        // Validar el stock de todos los items antes de descontar nada.
        for (ItemCarrito item : carrito.getItems()) {
            validarPublicado(item.getDisponibilidad().getVuelo());
            validarStock(item.getDisponibilidad(), asientosEnCarrito(carrito, item.getDisponibilidad()));
        }

        List<ItemOrden> itemsOrden = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        BigDecimal montoParaMillas = BigDecimal.ZERO; // lo gastado, multiplicado segun la clase

        for (ItemCarrito item : carrito.getItems()) {
            Disponibilidad disponibilidad = item.getDisponibilidad();
            BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
            // Ninos y bebes pagan un porcentaje de la tarifa (y del descuento) de un adulto
            TipoPasajero tipo = item.getTipoPasajero();
            BigDecimal precioUnitario = tipo.aplicar(disponibilidad.getPrecioConDescuento());
            BigDecimal descuentoUnitario = tipo.aplicar(disponibilidad.getDescuentoUnitario());

            disponibilidad.setAsientosDisponibles(
                    disponibilidad.getAsientosDisponibles() - item.getCantidad());
            disponibilidadRepository.save(disponibilidad);


            itemsOrden.add(ItemOrden.builder()
                    .disponibilidad(disponibilidad)
                    .cantidad(item.getCantidad())
                    .tipoPasajero(tipo)
                    .precioUnitario(precioUnitario)
                    .descuentoAplicado(descuentoUnitario)
                    .build());

            total = total.add(precioUnitario.multiply(cantidad));
            descuentoTotal = descuentoTotal.add(descuentoUnitario.multiply(cantidad));
            montoParaMillas = montoParaMillas.add(precioUnitario.multiply(cantidad)
                    .multiply(BigDecimal.valueOf(disponibilidad.getClase().getMultiplicadorMillas())));
        }

        // Millas: las que usa se descuentan del total, y solo suma millas lo que paga con plata
        Usuario usuario = carrito.getUsuario();
        int millasUsadas = millasAUsar == null ? 0 : millasAUsar;
        if (millasUsadas > usuario.getMillas()) {
            throw new BadRequestException("No tenes suficientes millas: tu saldo es de " + usuario.getMillas());
        }
        BigDecimal descuentoMillas = valorMilla.multiply(BigDecimal.valueOf(millasUsadas));
        if (descuentoMillas.compareTo(total) > 0) {
            throw new BadRequestException("Con esas millas pagarias mas que el total de la compra");
        }
        BigDecimal aPagar = total.subtract(descuentoMillas);
        int millasGanadas = total.signum() == 0 ? 0 : montoParaMillas
                .multiply(aPagar)
                .divide(total.multiply(pesosPorMilla), 0, RoundingMode.DOWN)
                .intValue();

        usuario.setMillas(usuario.getMillas() - millasUsadas + millasGanadas);
        usuarioRepository.save(usuario);

        Orden orden = Orden.builder()
                .usuario(carrito.getUsuario())
                .total(aPagar)
                .descuentoTotal(descuentoTotal)
                .millasGanadas(millasGanadas)
                .millasUsadas(millasUsadas)
                .descuentoMillas(descuentoMillas)
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
        if (!vuelo.estaOperativo()) {
            throw new BadRequestException("El vuelo " + vuelo.getNumeroVuelo() + " ya no esta disponible");
        }
        if (!vuelo.getFechaSalida().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("El vuelo " + vuelo.getNumeroVuelo() + " ya salio");
        }
    }

    private void validarStock(Disponibilidad disponibilidad, int cantidad) {
        if (cantidad > disponibilidad.getAsientosDisponibles()) {
            throw new BadRequestException("No hay suficientes asientos en clase "
                    + disponibilidad.getClase().getNombre() + " para el vuelo "
                    + disponibilidad.getVuelo().getNumeroVuelo());
        }
    }

    /** El carrito con las millas que sumaria la compra y el saldo que tiene para usar. */
    private CarritoResponse respuesta(Carrito carrito) {
        BigDecimal montoParaMillas = BigDecimal.ZERO;
        for (ItemCarrito item : carrito.getItems()) {
            Disponibilidad disponibilidad = item.getDisponibilidad();
            BigDecimal precioUnitario = item.getTipoPasajero().aplicar(disponibilidad.getPrecioConDescuento());
            montoParaMillas = montoParaMillas.add(precioUnitario
                    .multiply(BigDecimal.valueOf(item.getCantidad()))
                    .multiply(BigDecimal.valueOf(disponibilidad.getClase().getMultiplicadorMillas())));
        }

        return CarritoResponse.desde(carrito).toBuilder()
                .millasAGanar(montoParaMillas.divide(pesosPorMilla, 0, RoundingMode.DOWN).intValue())
                .millasDisponibles(carrito.getUsuario().getMillas())
                .valorMilla(valorMilla)
                .build();
    }

    /** Cuantos pasajeros hay en el carrito para ese vuelo y clase (sumando todos los tipos). */
    private int asientosEnCarrito(Carrito carrito, Disponibilidad disponibilidad) {
        return carrito.getItems().stream()
                .filter(item -> item.getDisponibilidad().getId().equals(disponibilidad.getId()))
                .mapToInt(ItemCarrito::getCantidad)
                .sum();
    }

    private ItemCarrito obtenerItemDelCarrito(Carrito carrito, Long itemId) {
        return carrito.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado en el carrito: " + itemId));
    }

    /**
     * Cada comprador tiene un unico carrito, y siempre se busca por el id que
     * viene en el JWT: nunca se recibe un id de carrito desde afuera, asi que
     * nadie puede ver ni tocar el carrito de otro. Si por algun motivo el
     * usuario todavia no tiene carrito, se le crea en ese momento.
     */
    private Carrito buscarCarrito(Long usuarioId) {
        return carritoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    Usuario usuario = usuarioRepository.findById(usuarioId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));
                    return carritoRepository.save(Carrito.builder().usuario(usuario).build());
                });
    }
}
