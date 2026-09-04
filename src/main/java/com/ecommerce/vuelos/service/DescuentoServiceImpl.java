package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.DescuentoRequest;
import com.ecommerce.vuelos.dto.vuelo.DescuentoResponse;
import com.ecommerce.vuelos.entity.Descuento;
import com.ecommerce.vuelos.entity.TipoDescuento;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.DescuentoRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DescuentoServiceImpl implements DescuentoService {

    private final DescuentoRepository descuentoRepository;
    private final VueloRepository vueloRepository;

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
        Vuelo vuelo = buscarVuelo(request.getVueloId());
        validarPropiedad(vuelo, principal);
        validarRequest(request);
        validarSolapamiento(vuelo.getId(), request, null);

        Descuento descuento = Descuento.builder()
                .vuelo(vuelo)
                .tipoDescuento(request.getTipoDescuento())
                .valor(request.getValor())
                .fechaDesde(request.getFechaDesde())
                .fechaHasta(request.getFechaHasta())
                .activo(request.getActivo())
                .build();

        Descuento guardado = descuentoRepository.save(descuento);

        // Igual que con Disponibilidad: hay que actualizar la coleccion en memoria
        // del vuelo para que un getPrecioConDescuento() posterior, en la misma
        // transaccion, ya vea este descuento.
        vuelo.getDescuentos().add(guardado);

        return DescuentoResponse.desde(guardado);
    }

    @Override
    @Transactional
    public DescuentoResponse actualizar(Long id, DescuentoRequest request, UsuarioPrincipal principal) {
        Descuento descuento = buscarPorId(id);
        validarPropiedad(descuento.getVuelo(), principal);
        validarRequest(request);
        validarSolapamiento(descuento.getVuelo().getId(), request, id);

        descuento.setTipoDescuento(request.getTipoDescuento());
        descuento.setValor(request.getValor());
        descuento.setFechaDesde(request.getFechaDesde());
        descuento.setFechaHasta(request.getFechaHasta());
        descuento.setActivo(request.getActivo());

        return DescuentoResponse.desde(descuentoRepository.save(descuento));
    }

    private void validarRequest(DescuentoRequest request) {
        if (request.getFechaHasta().isBefore(request.getFechaDesde())) {
            throw new BadRequestException("La fecha hasta debe ser posterior o igual a la fecha desde");
        }
        if (request.getTipoDescuento() == TipoDescuento.PORCENTAJE
                && request.getValor().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("El descuento porcentual no puede superar el 100%");
        }
    }

    /** No puede haber dos descuentos activos del mismo vuelo con fechas superpuestas. */
    private void validarSolapamiento(Long vueloId, DescuentoRequest request, Long idAExcluir) {
        if (!Boolean.TRUE.equals(request.getActivo())) {
            return;
        }
        boolean solapa = descuentoRepository.findByVueloIdAndActivoTrue(vueloId).stream()
                .filter(d -> !d.getId().equals(idAExcluir))
                .anyMatch(d -> !request.getFechaDesde().isAfter(d.getFechaHasta())
                        && !d.getFechaDesde().isAfter(request.getFechaHasta()));
        if (solapa) {
            throw new BadRequestException("Ya existe un descuento activo para ese vuelo en ese rango de fechas");
        }
    }

    private Descuento buscarPorId(Long id) {
        return descuentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Descuento no encontrado: " + id));
    }

    private Vuelo buscarVuelo(Long id) {
        return vueloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vuelo no encontrado: " + id));
    }

    private void validarPropiedad(Vuelo vuelo, UsuarioPrincipal principal) {
        if (principal.esAdmin()) {
            return;
        }
        if (!vuelo.getVendedor().getId().equals(principal.getId())) {
            throw new BadRequestException("Solo el vendedor que publico el vuelo puede manejar sus descuentos");
        }
    }
}
