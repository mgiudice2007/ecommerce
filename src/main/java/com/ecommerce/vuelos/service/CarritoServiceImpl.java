package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.carrito.AsientosRequest;
import com.ecommerce.vuelos.dto.carrito.CarritoResponse;
import com.ecommerce.vuelos.dto.carrito.ItemCarritoRequest;
import com.ecommerce.vuelos.dto.carrito.PasajesRequest;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.entity.AsientoOcupado;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @Autowired
    private AsientoService asientoService;

    // Reglas de las millas, configurables en application.properties (no estan fijas en el codigo)
    @Value("${millas.pesos-por-milla}")
    private BigDecimal pesosPorMilla;
    @Value("${millas.valor-en-pesos}")
    private BigDecimal valorMilla;
    @Value("${ordenes.horas-para-cancelar}")
    private int horasParaCancelar;

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
        // Adultos y niños del mismo vuelo y clase ocupan asientos del mismo cupo; los bebés viajan a upa
        int asientosNuevos = tipo.ocupaAsiento() ? request.getCantidad() : 0;
        validarStock(disponibilidad, asientosEnCarrito(carrito, disponibilidad) + asientosNuevos);

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
    public CarritoResponse agregarPasajes(Long usuarioId, PasajesRequest request) {
        if (request.getAdultos() + request.getNinos() + request.getBebes() == 0) {
            throw new BadRequestException("Elegí al menos un pasajero");
        }
        if (request.getAdultos() == 0) {
            throw new BadRequestException("Tiene que viajar al menos un adulto");
        }
        if (request.getBebes() > request.getAdultos()) {
            throw new BadRequestException("Cada bebé tiene que viajar con un adulto");
        }

        // Un item por cada tipo de pasajero. Si algo falla, la transaccion deshace todo.
        CarritoResponse carrito = null;
        if (request.getAdultos() > 0) {
            carrito = agregarItem(usuarioId, new ItemCarritoRequest(
                    request.getDisponibilidadId(), request.getAdultos(), TipoPasajero.ADULTO));
        }
        if (request.getNinos() > 0) {
            carrito = agregarItem(usuarioId, new ItemCarritoRequest(
                    request.getDisponibilidadId(), request.getNinos(), TipoPasajero.NINO));
        }
        if (request.getBebes() > 0) {
            carrito = agregarItem(usuarioId, new ItemCarritoRequest(
                    request.getDisponibilidadId(), request.getBebes(), TipoPasajero.BEBE));
        }

        // Si eligio asientos en el detalle del vuelo, se guardan junto con los que ya
        // tenia ese vuelo y clase en el carrito (se reparten primero entre los adultos)
        if (request.getAsientos() != null && !request.getAsientos().isEmpty()) {
            Carrito actualizado = buscarCarrito(usuarioId);
            Disponibilidad disponibilidad = disponibilidadRepository.findById(request.getDisponibilidadId()).orElseThrow();
            List<String> todos = new ArrayList<>();
            for (ItemCarrito item : itemsConAsiento(actualizado, disponibilidad)) {
                todos.addAll(item.getAsientos());
            }
            todos.addAll(request.getAsientos());
            carrito = elegirAsientos(usuarioId, new AsientosRequest(request.getDisponibilidadId(), todos));
        }
        return carrito;
    }

    @Override
    @Transactional
    public CarritoResponse actualizarItem(Long usuarioId, Long itemId, Integer cantidad) {
        Carrito carrito = buscarCarrito(usuarioId);
        ItemCarrito item = obtenerItemDelCarrito(carrito, itemId);

        if (item.getTipoPasajero().ocupaAsiento()) {
            int otrosPasajeros = asientosEnCarrito(carrito, item.getDisponibilidad()) - item.getCantidad();
            validarStock(item.getDisponibilidad(), otrosPasajeros + cantidad);
        }
        item.setCantidad(cantidad);
        // Si bajo la cantidad, sobran asientos elegidos: se quedan los primeros
        while (item.getAsientos().size() > cantidad) {
            item.getAsientos().remove(item.getAsientos().size() - 1);
        }
        carritoRepository.save(carrito);
        return respuesta(carrito);
    }

    @Override
    @Transactional
    public CarritoResponse elegirAsientos(Long usuarioId, AsientosRequest request) {
        Carrito carrito = buscarCarrito(usuarioId);
        Disponibilidad disponibilidad = disponibilidadRepository.findById(request.getDisponibilidadId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Disponibilidad no encontrada: " + request.getDisponibilidadId()));

        // Los pasajeros que necesitan asiento: primero los adultos y despues los niños
        List<ItemCarrito> conAsiento = itemsConAsiento(carrito, disponibilidad);
        int pasajeros = conAsiento.stream().mapToInt(ItemCarrito::getCantidad).sum();
        if (pasajeros == 0) {
            throw new BadRequestException("No hay pasajeros en el carrito para ese vuelo y clase");
        }

        List<String> elegidos = request.getAsientos();
        if (elegidos.size() > pasajeros) {
            throw new BadRequestException("Elegiste " + elegidos.size() + " asientos pero hay "
                    + pasajeros + " pasajeros");
        }
        if (new HashSet<>(elegidos).size() != elegidos.size()) {
            throw new BadRequestException("Elegiste el mismo asiento dos veces");
        }
        List<String> existentes = asientoService.codigos(disponibilidad);
        Set<String> ocupados = asientoService.ocupados(disponibilidad.getId());
        for (String codigo : elegidos) {
            if (!existentes.contains(codigo)) {
                throw new BadRequestException("El asiento " + codigo + " no existe en esta clase");
            }
            if (ocupados.contains(codigo)) {
                throw new BadRequestException("El asiento " + codigo + " ya está ocupado, elegí otro");
            }
        }

        // Se reparten en orden: los primeros asientos para los adultos y el resto para los niños
        int siguiente = 0;
        for (ItemCarrito item : conAsiento) {
            item.getAsientos().clear();
            while (item.getAsientos().size() < item.getCantidad() && siguiente < elegidos.size()) {
                item.getAsientos().add(elegidos.get(siguiente));
                siguiente++;
            }
        }

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
            validarAdultos(carrito, item.getDisponibilidad());
        }

        // Asientos: los que eligio tienen que seguir libres, y a los pasajeros que no
        // eligieron se les asignan asientos libres, como en el check-in de las aerolineas
        Map<Long, Set<String>> tomados = new HashMap<>();
        for (ItemCarrito item : carrito.getItems()) {
            Disponibilidad disponibilidad = item.getDisponibilidad();
            Set<String> tomadosDelVuelo = tomados.computeIfAbsent(disponibilidad.getId(),
                    id -> new HashSet<>(asientoService.ocupados(id)));
            for (String codigo : item.getAsientos()) {
                if (!tomadosDelVuelo.add(codigo)) {
                    throw new BadRequestException("El asiento " + codigo + " del vuelo "
                            + disponibilidad.getVuelo().getNumeroVuelo() + " ya fue tomado, elegí otro");
                }
            }
        }
        for (ItemCarrito item : carrito.getItems()) {
            if (!item.getTipoPasajero().ocupaAsiento()) {
                continue;
            }
            Set<String> tomadosDelVuelo = tomados.get(item.getDisponibilidad().getId());
            for (String codigo : asientoService.codigos(item.getDisponibilidad())) {
                if (item.getAsientos().size() >= item.getCantidad()) {
                    break;
                }
                if (tomadosDelVuelo.add(codigo)) {
                    item.getAsientos().add(codigo);
                }
            }
        }

        List<ItemOrden> itemsOrden = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        BigDecimal montoParaMillas = BigDecimal.ZERO; // lo gastado, multiplicado segun la clase

        for (ItemCarrito item : carrito.getItems()) {
            Disponibilidad disponibilidad = item.getDisponibilidad();
            BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
            // Niños y bebés pagan un porcentaje de la tarifa (y del descuento) de un adulto
            TipoPasajero tipo = item.getTipoPasajero();
            BigDecimal precioUnitario = tipo.aplicar(disponibilidad.getPrecioConDescuento());
            BigDecimal descuentoUnitario = tipo.aplicar(disponibilidad.getDescuentoUnitario());

            // Los bebés viajan a upa: no descuentan asientos del cupo
            if (tipo.ocupaAsiento()) {
                disponibilidad.setAsientosDisponibles(
                        disponibilidad.getAsientosDisponibles() - item.getCantidad());
                disponibilidadRepository.save(disponibilidad);
            }

            ItemOrden itemOrden = ItemOrden.builder()
                    .disponibilidad(disponibilidad)
                    .cantidad(item.getCantidad())
                    .tipoPasajero(tipo)
                    .precioUnitario(precioUnitario)
                    .descuentoAplicado(descuentoUnitario)
                    .build();
            for (String codigo : item.getAsientos()) {
                itemOrden.getAsientos().add(AsientoOcupado.builder()
                        .disponibilidad(disponibilidad)
                        .itemOrden(itemOrden)
                        .codigo(codigo)
                        .build());
            }
            itemsOrden.add(itemOrden);

            total = total.add(precioUnitario.multiply(cantidad));
            descuentoTotal = descuentoTotal.add(descuentoUnitario.multiply(cantidad));
            montoParaMillas = montoParaMillas.add(precioUnitario.multiply(cantidad)
                    .multiply(BigDecimal.valueOf(disponibilidad.getClase().getMultiplicadorMillas())));
        }

        // Millas: las que usa se descuentan del total, y solo suma millas lo que paga con plata
        Usuario usuario = carrito.getUsuario();
        int millasUsadas = millasAUsar == null ? 0 : millasAUsar;
        if (millasUsadas > usuario.getMillas()) {
            throw new BadRequestException("No tenés suficientes millas: tu saldo es de " + usuario.getMillas());
        }
        BigDecimal descuentoMillas = valorMilla.multiply(BigDecimal.valueOf(millasUsadas));
        if (descuentoMillas.compareTo(total) > 0) {
            throw new BadRequestException("Con esas millas pagarías más que el total de la compra");
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

        return OrdenResponse.desde(guardada, horasParaCancelar);
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

        int millasAGanar = montoParaMillas.divide(pesosPorMilla, 0, RoundingMode.DOWN).intValue();
        return CarritoResponse.desde(carrito, millasAGanar, carrito.getUsuario().getMillas(), valorMilla);
    }

    /** Cuantos asientos necesitan los pasajeros del carrito para ese vuelo y clase (los bebés no cuentan). */
    private int asientosEnCarrito(Carrito carrito, Disponibilidad disponibilidad) {
        return itemsConAsiento(carrito, disponibilidad).stream()
                .mapToInt(ItemCarrito::getCantidad)
                .sum();
    }

    /** Los items de ese vuelo y clase que necesitan asiento: primero los adultos y despues los niños. */
    private List<ItemCarrito> itemsConAsiento(Carrito carrito, Disponibilidad disponibilidad) {
        List<ItemCarrito> items = new ArrayList<>();
        for (TipoPasajero tipo : List.of(TipoPasajero.ADULTO, TipoPasajero.NINO)) {
            for (ItemCarrito item : carrito.getItems()) {
                if (item.getDisponibilidad().getId().equals(disponibilidad.getId()) && item.getTipoPasajero() == tipo) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    /**
     * En cada vuelo y clase tiene que viajar al menos un adulto (los menores no viajan
     * solos) y cada bebé viaja a upa de un adulto.
     */
    private void validarAdultos(Carrito carrito, Disponibilidad disponibilidad) {
        int adultos = 0;
        int bebes = 0;
        for (ItemCarrito item : carrito.getItems()) {
            if (item.getDisponibilidad().getId().equals(disponibilidad.getId())) {
                if (item.getTipoPasajero() == TipoPasajero.ADULTO) {
                    adultos += item.getCantidad();
                }
                if (item.getTipoPasajero() == TipoPasajero.BEBE) {
                    bebes += item.getCantidad();
                }
            }
        }
        if (adultos == 0) {
            throw new BadRequestException("Tiene que viajar al menos un adulto en el vuelo "
                    + disponibilidad.getVuelo().getNumeroVuelo());
        }
        if (bebes > adultos) {
            throw new BadRequestException("Cada bebé tiene que viajar con un adulto en el vuelo "
                    + disponibilidad.getVuelo().getNumeroVuelo());
        }
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
