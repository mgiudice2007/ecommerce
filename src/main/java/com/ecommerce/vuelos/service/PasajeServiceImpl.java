package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.carrito.PasajeroRequest;
import com.ecommerce.vuelos.entity.Carrito;
import com.ecommerce.vuelos.entity.ItemCarrito;
import com.ecommerce.vuelos.entity.ItemOrden;
import com.ecommerce.vuelos.entity.Pasaje;
import com.ecommerce.vuelos.entity.TipoDocumento;
import com.ecommerce.vuelos.entity.TipoPasajero;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.repository.PasajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PasajeServiceImpl implements PasajeService {

    // Edades de las aerolineas: bebe hasta 1 año, niño de 2 a 11 y adulto desde los 12
    private static final int EDAD_MAXIMA_BEBE = 1;
    private static final int EDAD_MAXIMA_NINO = 11;

    // Sin 0/O ni 1/I/L para que el codigo se pueda dictar sin confusiones
    private static final String LETRAS_CODIGO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int LARGO_CODIGO = 8;
    private static final SecureRandom AZAR = new SecureRandom();

    @Autowired
    private PasajeRepository pasajeRepository;

    @Override
    public Map<Long, List<PasajeroRequest>> validar(Carrito carrito, List<PasajeroRequest> pasajeros) {
        if (pasajeros == null || pasajeros.isEmpty()) {
            throw new BadRequestException("Cargá los datos de los pasajeros antes de pagar");
        }

        Map<Long, List<PasajeroRequest>> porItem = new HashMap<>();
        for (PasajeroRequest pasajero : pasajeros) {
            porItem.computeIfAbsent(pasajero.getItemId(), id -> new ArrayList<>()).add(pasajero);
        }

        Set<Long> itemsDelCarrito = new HashSet<>();
        Map<Long, Set<String>> documentosPorVuelo = new HashMap<>();
        for (ItemCarrito item : carrito.getItems()) {
            itemsDelCarrito.add(item.getId());
            List<PasajeroRequest> delItem = porItem.getOrDefault(item.getId(), List.of());
            String numeroVuelo = item.getDisponibilidad().getVuelo().getNumeroVuelo();
            if (delItem.size() != item.getCantidad()) {
                throw new BadRequestException("Faltan datos de pasajeros: el vuelo " + numeroVuelo + " tiene "
                        + item.getCantidad() + " lugar(es) de " + nombreTipo(item.getTipoPasajero())
                        + " y se cargaron " + delItem.size());
            }
            LocalDate diaDelVuelo = item.getDisponibilidad().getVuelo().getFechaSalida().toLocalDate();
            Set<String> documentos = documentosPorVuelo.computeIfAbsent(
                    item.getDisponibilidad().getVuelo().getId(), id -> new HashSet<>());
            for (PasajeroRequest pasajero : delItem) {
                validarDocumento(pasajero);
                validarEdad(pasajero, item.getTipoPasajero(), diaDelVuelo);
                String persona = pasajero.getTipoDocumento() + ":" + pasajero.getDocumento().toUpperCase();
                if (!documentos.add(persona)) {
                    throw new BadRequestException("El documento " + pasajero.getDocumento()
                            + " está repetido en el vuelo " + numeroVuelo);
                }
                // Como en las aerolineas reales: una persona no puede tener dos pasajes en el mismo vuelo
                if (pasajeRepository.existeEnVuelo(item.getDisponibilidad().getVuelo().getId(),
                        pasajero.getTipoDocumento(), pasajero.getDocumento().toUpperCase())) {
                    throw new BadRequestException(pasajero.getNombre().trim() + " " + pasajero.getApellido().trim()
                            + " (" + pasajero.getTipoDocumento() + " " + pasajero.getDocumento()
                            + ") ya tiene un pasaje en el vuelo " + numeroVuelo);
                }
            }
        }

        // Pasajeros que apuntan a un pasaje que no esta en el carrito
        if (!itemsDelCarrito.containsAll(porItem.keySet())) {
            throw new BadRequestException("Hay pasajeros que no corresponden a ningún pasaje del carrito");
        }
        return porItem;
    }

    @Override
    public List<Pasaje> crearPasajes(ItemOrden itemOrden, ItemCarrito item, List<PasajeroRequest> pasajeros) {
        List<Pasaje> pasajes = new ArrayList<>();
        for (int i = 0; i < pasajeros.size(); i++) {
            PasajeroRequest pasajero = pasajeros.get(i);
            // La persona i va en el asiento i del pasaje (los bebes no tienen asiento)
            String asiento = item.getTipoPasajero().ocupaAsiento() && i < item.getAsientos().size()
                    ? item.getAsientos().get(i)
                    : null;
            pasajes.add(Pasaje.builder()
                    .itemOrden(itemOrden)
                    .nombre(pasajero.getNombre().trim())
                    .apellido(pasajero.getApellido().trim())
                    .tipoDocumento(pasajero.getTipoDocumento())
                    .documento(pasajero.getDocumento().toUpperCase())
                    .fechaNacimiento(pasajero.getFechaNacimiento())
                    .asiento(asiento)
                    .codigoEmbarque(codigoNuevo())
                    .build());
        }
        return pasajes;
    }

    private void validarDocumento(PasajeroRequest pasajero) {
        boolean soloNumeros = pasajero.getDocumento().chars().allMatch(Character::isDigit);
        if (pasajero.getTipoDocumento() == TipoDocumento.DNI
                && (!soloNumeros || pasajero.getDocumento().length() < 7 || pasajero.getDocumento().length() > 8)) {
            throw new BadRequestException("El DNI de " + pasajero.getNombre() + " tiene que tener 7 u 8 números");
        }
    }

    private void validarEdad(PasajeroRequest pasajero, TipoPasajero tipo, LocalDate diaDelVuelo) {
        int edad = Period.between(pasajero.getFechaNacimiento(), diaDelVuelo).getYears();
        boolean corresponde = switch (tipo) {
            case BEBE -> edad <= EDAD_MAXIMA_BEBE;
            case NINO -> edad > EDAD_MAXIMA_BEBE && edad <= EDAD_MAXIMA_NINO;
            case ADULTO -> edad > EDAD_MAXIMA_NINO;
        };
        if (!corresponde) {
            throw new BadRequestException(pasajero.getNombre() + " " + pasajero.getApellido() + " tendría "
                    + Math.max(edad, 0) + " años en la fecha del vuelo y se cargó como " + nombreTipo(tipo)
                    + " (" + rangoDeEdad(tipo) + ")");
        }
    }

    private String rangoDeEdad(TipoPasajero tipo) {
        return switch (tipo) {
            case BEBE -> "hasta 1 año";
            case NINO -> "de 2 a 11 años";
            case ADULTO -> "desde los 12 años";
        };
    }

    private String nombreTipo(TipoPasajero tipo) {
        return switch (tipo) {
            case ADULTO -> "adulto";
            case NINO -> "niño";
            case BEBE -> "bebé";
        };
    }

    private String codigoNuevo() {
        String codigo;
        do {
            StringBuilder armado = new StringBuilder("BCA-");
            for (int i = 0; i < LARGO_CODIGO; i++) {
                armado.append(LETRAS_CODIGO.charAt(AZAR.nextInt(LETRAS_CODIGO.length())));
            }
            codigo = armado.toString();
        } while (pasajeRepository.existsByCodigoEmbarque(codigo));
        return codigo;
    }
}
