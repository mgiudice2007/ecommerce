package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.FilaAsientosResponse;
import com.ecommerce.vuelos.dto.vuelo.MapaAsientosResponse;
import com.ecommerce.vuelos.entity.AsientoOcupado;
import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.AsientoOcupadoRepository;
import com.ecommerce.vuelos.repository.DisponibilidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Arma el mapa de asientos de cada clase. Las filas salen de la cantidad de asientos
 * de la clase y de cuantos asientos tiene cada fila (dato de la tabla clases:
 * Economica 6, Ejecutiva 4, Primera 2). Como en un avion de verdad, las cabinas con
 * menos asientos por fila van adelante, asi que las filas se numeran de corrido:
 * primero Primera, despues Ejecutiva y al final Economica.
 */
@Service
public class AsientoServiceImpl implements AsientoService {

    private static final int ASIENTOS_POR_FILA_SI_NO_HAY_DATO = 6;

    @Autowired
    private DisponibilidadRepository disponibilidadRepository;
    @Autowired
    private AsientoOcupadoRepository asientoOcupadoRepository;

    @Override
    @Transactional(readOnly = true)
    public MapaAsientosResponse mapa(Long disponibilidadId) {
        Disponibilidad disponibilidad = disponibilidadRepository.findById(disponibilidadId)
                .orElseThrow(() -> new ResourceNotFoundException("Disponibilidad no encontrada: " + disponibilidadId));

        List<String> letras = letras(disponibilidad);
        List<String> codigos = codigos(disponibilidad);
        int primeraFila = primeraFila(disponibilidad);

        // Los codigos se cortan de a "letras.size()" para formar cada fila
        List<FilaAsientosResponse> filas = new ArrayList<>();
        for (int i = 0; i < codigos.size(); i += letras.size()) {
            int hasta = Math.min(i + letras.size(), codigos.size());
            filas.add(new FilaAsientosResponse(primeraFila + i / letras.size(), codigos.subList(i, hasta)));
        }

        return MapaAsientosResponse.builder()
                .disponibilidadId(disponibilidad.getId())
                .claseNombre(disponibilidad.getClase().getNombre())
                .letras(letras)
                .filas(filas)
                .ocupados(new ArrayList<>(ocupados(disponibilidad.getId())))
                .build();
    }

    @Override
    public List<String> codigos(Disponibilidad disponibilidad) {
        List<String> letras = letras(disponibilidad);
        int primeraFila = primeraFila(disponibilidad);

        List<String> codigos = new ArrayList<>();
        for (int i = 0; i < disponibilidad.getAsientosTotales(); i++) {
            int fila = primeraFila + i / letras.size();
            codigos.add(fila + letras.get(i % letras.size()));
        }
        return codigos;
    }

    @Override
    public Set<String> ocupados(Long disponibilidadId) {
        Set<String> ocupados = new TreeSet<>();
        for (AsientoOcupado asiento : asientoOcupadoRepository.findByDisponibilidadId(disponibilidadId)) {
            ocupados.add(asiento.getCodigo());
        }
        return ocupados;
    }

    /** Las letras de cada fila: 6 = A B C D E F, 4 = A C D F, 2 = A F (las de ventanilla y pasillo). */
    private List<String> letras(Disponibilidad disponibilidad) {
        return switch (asientosPorFila(disponibilidad)) {
            case 2 -> List.of("A", "F");
            case 4 -> List.of("A", "C", "D", "F");
            case 6 -> List.of("A", "B", "C", "D", "E", "F");
            default -> {
                List<String> letras = new ArrayList<>();
                for (int i = 0; i < asientosPorFila(disponibilidad); i++) {
                    letras.add(String.valueOf((char) ('A' + i)));
                }
                yield letras;
            }
        };
    }

    private int asientosPorFila(Disponibilidad disponibilidad) {
        Integer porFila = disponibilidad.getClase().getAsientosPorFila();
        return porFila == null || porFila < 1 ? ASIENTOS_POR_FILA_SI_NO_HAY_DATO : porFila;
    }

    private int cantidadDeFilas(Disponibilidad disponibilidad) {
        int porFila = asientosPorFila(disponibilidad);
        return (disponibilidad.getAsientosTotales() + porFila - 1) / porFila;
    }

    /** La fila donde arranca esta clase: despues de todas las cabinas que van adelante. */
    private int primeraFila(Disponibilidad disponibilidad) {
        int fila = 1;
        for (Disponibilidad otra : disponibilidadRepository.findByVueloId(disponibilidad.getVuelo().getId())) {
            if (vaAdelante(otra, disponibilidad)) {
                fila += cantidadDeFilas(otra);
            }
        }
        return fila;
    }

    private boolean vaAdelante(Disponibilidad otra, Disponibilidad disponibilidad) {
        if (otra.getId().equals(disponibilidad.getId())) {
            return false;
        }
        int porFilaOtra = asientosPorFila(otra);
        int porFila = asientosPorFila(disponibilidad);
        if (porFilaOtra != porFila) {
            return porFilaOtra < porFila;
        }
        return otra.getClase().getId() < disponibilidad.getClase().getId();
    }
}
