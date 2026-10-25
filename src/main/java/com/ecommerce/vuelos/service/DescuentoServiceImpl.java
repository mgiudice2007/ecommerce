package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.descuento.DescuentoRequest;
import com.ecommerce.vuelos.dto.descuento.DescuentoResponse;
import com.ecommerce.vuelos.entity.Descuento;
import com.ecommerce.vuelos.entity.TipoDescuento;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.DescuentoRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DescuentoServiceImpl implements DescuentoService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    @Autowired
    private DescuentoRepository descuentoRepository;
    @Autowired
    private VueloRepository vueloRepository;

    @Override
    public List<DescuentoResponse> listarPorVuelo(Long vueloId) {
        return descuentoRepository.findByVueloId(vueloId).stream()
                .map(DescuentoResponse::desde)
                .toList();
    }

    @Override
    public DescuentoResponse obtener(Long id) {
        return DescuentoResponse.desde(buscarPorId(id));
    }

    @Override
    @Transactional
    public DescuentoResponse crear(DescuentoRequest request, UsuarioPrincipal principal) {
        Vuelo vuelo = vueloRepository.findById(request.getVueloId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vuelo no encontrado: " + request.getVueloId()));
        validarPropiedad(vuelo, principal);
        validarRequest(request, vuelo);
        validarFechas(request, vuelo, null);
        validarSinSolapamiento(request, vuelo, null);

        Descuento descuento = Descuento.builder()
                .vuelo(vuelo)
                .tipoDescuento(request.getTipoDescuento())
                .valor(request.getValor())
                .fechaDesde(request.getFechaDesde())
                .fechaHasta(request.getFechaHasta())
                .activo(request.getActivo() == null || request.getActivo())
                .build();

        Descuento guardado = descuentoRepository.save(descuento);

        vuelo.getDescuentos().add(guardado);

        return DescuentoResponse.desde(guardado);
    }

    @Override
    @Transactional
    public DescuentoResponse actualizar(Long id, DescuentoRequest request, UsuarioPrincipal principal) {
        Descuento descuento = buscarPorId(id);
        Vuelo vuelo = descuento.getVuelo();
        validarPropiedad(vuelo, principal);
        validarRequest(request, vuelo);
        validarFechas(request, vuelo, descuento);
        validarSinSolapamiento(request, vuelo, id);

        descuento.setTipoDescuento(request.getTipoDescuento());
        descuento.setValor(request.getValor());
        descuento.setFechaDesde(request.getFechaDesde());
        descuento.setFechaHasta(request.getFechaHasta());
        if (request.getActivo() != null) {
            descuento.setActivo(request.getActivo());
        }

        return DescuentoResponse.desde(descuentoRepository.save(descuento));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UsuarioPrincipal principal) {
        Descuento descuento = buscarPorId(id);
        validarPropiedad(descuento.getVuelo(), principal);
        descuento.getVuelo().getDescuentos().remove(descuento);
        descuentoRepository.delete(descuento); // lo borra de la base de verdad
    }


    private void validarRequest(DescuentoRequest request, Vuelo vuelo) {
        if (request.getFechaHasta().isBefore(request.getFechaDesde())) {
            throw new BadRequestException("La fecha de fin no puede ser anterior a la de inicio");
        }
        if (request.getTipoDescuento() == TipoDescuento.PORCENTAJE
                && request.getValor().compareTo(CIEN) > 0) {
            throw new BadRequestException("Un descuento porcentual no puede superar el 100%");
        }
   
        if (request.getTipoDescuento() == TipoDescuento.MONTO_FIJO
                && request.getValor().compareTo(vuelo.getPrecio()) > 0) {
            throw new BadRequestException("El monto fijo (" + request.getValor()
                    + ") no puede superar el precio del vuelo (" + vuelo.getPrecio() + ")");
        }
    }

    /**
     * El descuento tiene que caer entre hoy y el dia que sale el vuelo: no puede empezar
     * en una fecha pasada ni seguir despues de que el vuelo salio.
     * Al editar solo se controla la fecha que cambio, asi se puede pausar o activar un
     * descuento que empezo hace unos dias.
     */
    private void validarFechas(DescuentoRequest request, Vuelo vuelo, Descuento actual) {
        boolean cambioDesde = actual == null || !request.getFechaDesde().equals(actual.getFechaDesde());
        boolean cambioHasta = actual == null || !request.getFechaHasta().equals(actual.getFechaHasta());
        LocalDate salida = vuelo.getFechaSalida().toLocalDate();

        if (cambioDesde && request.getFechaDesde().isBefore(LocalDate.now())) {
            throw new BadRequestException("La fecha de inicio del descuento no puede ser anterior a hoy");
        }
        if (cambioHasta && request.getFechaHasta().isAfter(salida)) {
            throw new BadRequestException("El descuento tiene que terminar como tarde el dia que sale el vuelo ("
                    + salida.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
        }
    }

    /** No se permiten dos descuentos que compartan dias sobre el mismo vuelo. */
    private void validarSinSolapamiento(DescuentoRequest request, Vuelo vuelo, Long idQueSeEdita) {
        boolean hayChoque = descuentoRepository.findByVueloId(vuelo.getId()).stream()
                .filter(d -> !d.getId().equals(idQueSeEdita))
                .filter(d -> Boolean.TRUE.equals(d.getActivo()))
                .anyMatch(d -> d.seSolapaCon(request.getFechaDesde(), request.getFechaHasta()));

        if (hayChoque) {
            throw new BadRequestException(
                    "Ya hay un descuento activo para ese vuelo que se superpone con esas fechas");
        }
    }

    private Descuento buscarPorId(Long id) {
        return descuentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento no encontrado: " + id));
    }

    private void validarPropiedad(Vuelo vuelo, UsuarioPrincipal principal) {
        if (principal.esAdmin()) {
            return;
        }
        if (!vuelo.getVendedor().getId().equals(principal.getId())) {
            throw new BadRequestException(
                    "Solo el vendedor que publico el vuelo puede manejar sus descuentos");
        }
    }
}
