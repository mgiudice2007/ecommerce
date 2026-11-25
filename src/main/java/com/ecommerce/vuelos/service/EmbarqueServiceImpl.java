package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.embarque.EmbarqueResponse;
import com.ecommerce.vuelos.entity.EstadoEmbarque;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.EstadoVuelo;
import com.ecommerce.vuelos.entity.Pasaje;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.PasajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmbarqueServiceImpl implements EmbarqueService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private PasajeRepository pasajeRepository;

    @Override
    @Transactional(readOnly = true)
    public EmbarqueResponse validar(String codigoEmbarque) {
        Pasaje pasaje = buscar(codigoEmbarque);
        return EmbarqueResponse.desde(pasaje, estadoDe(pasaje), motivoDe(pasaje));
    }

    @Override
    @Transactional
    public EmbarqueResponse embarcar(String codigoEmbarque) {
        Pasaje pasaje = buscar(codigoEmbarque);
        EstadoEmbarque estado = estadoDe(pasaje);
        if (estado != EstadoEmbarque.VALIDO) {
            throw new BadRequestException("No se puede embarcar: " + motivoDe(pasaje));
        }
        pasaje.setEmbarcadoEn(LocalDateTime.now());
        pasajeRepository.save(pasaje);
        return EmbarqueResponse.desde(pasaje, EstadoEmbarque.EMBARCADO, motivoDe(pasaje));
    }

    private Pasaje buscar(String codigoEmbarque) {
        // El QR puede traer el codigo en minusculas o con espacios al tipearlo a mano
        String codigo = codigoEmbarque == null ? "" : codigoEmbarque.trim().toUpperCase();
        return pasajeRepository.findByCodigoEmbarque(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ningún pasaje con el código " + codigo));
    }

    private EstadoEmbarque estadoDe(Pasaje pasaje) {
        if (pasaje.getItemOrden().getOrden().getEstado() == EstadoOrden.CANCELADA
                || pasaje.getItemOrden().getDisponibilidad().getVuelo().getEstado() == EstadoVuelo.CANCELADO) {
            return EstadoEmbarque.ANULADO;
        }
        return pasaje.getEmbarcadoEn() != null ? EstadoEmbarque.EMBARCADO : EstadoEmbarque.VALIDO;
    }

    private String motivoDe(Pasaje pasaje) {
        return switch (estadoDe(pasaje)) {
            case ANULADO -> pasaje.getItemOrden().getOrden().getEstado() == EstadoOrden.CANCELADA
                    ? "la compra fue cancelada"
                    : "el vuelo fue cancelado";
            case EMBARCADO -> "ya embarcó el " + HORA.format(pasaje.getEmbarcadoEn());
            case VALIDO -> null;
        };
    }
}
