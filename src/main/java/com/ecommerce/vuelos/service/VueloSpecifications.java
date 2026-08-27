package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.model.ClaseVuelo;
import com.ecommerce.vuelos.model.Vuelo;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class VueloSpecifications {

    private VueloSpecifications() {
    }

    public static Specification<Vuelo> origenContiene(String origen) {
        return (root, query, cb) -> origen == null ? null :
                cb.like(cb.lower(root.get("origen")), "%" + origen.toLowerCase() + "%");
    }

    public static Specification<Vuelo> destinoContiene(String destino) {
        return (root, query, cb) -> destino == null ? null :
                cb.like(cb.lower(root.get("destino")), "%" + destino.toLowerCase() + "%");
    }

    public static Specification<Vuelo> esClase(ClaseVuelo clase) {
        return (root, query, cb) -> clase == null ? null : cb.equal(root.get("clase"), clase);
    }

    public static Specification<Vuelo> precioMinimo(BigDecimal precioMin) {
        return (root, query, cb) -> precioMin == null ? null :
                cb.greaterThanOrEqualTo(root.get("precio"), precioMin);
    }

    public static Specification<Vuelo> precioMaximo(BigDecimal precioMax) {
        return (root, query, cb) -> precioMax == null ? null :
                cb.lessThanOrEqualTo(root.get("precio"), precioMax);
    }
}
