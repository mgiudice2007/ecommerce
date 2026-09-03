package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.entity.ClaseVuelo;
import com.ecommerce.vuelos.entity.EstadoVuelo;
import com.ecommerce.vuelos.entity.Vuelo;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class VueloSpecifications {

    private VueloSpecifications() {
    }

    /** Un vuelo dado de baja no se lista nunca. */
    public static Specification<Vuelo> soloPublicados() {
        return (root, query, cb) -> cb.notEqual(root.get("estado"), EstadoVuelo.ELIMINADO);
    }

    public static Specification<Vuelo> origenContiene(String origen) {
        return (root, query, cb) -> {
            if (origen == null) {
                return null;
            }
            String patron = "%" + origen.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("origen").get("ciudad")), patron),
                    cb.like(cb.lower(root.get("origen").get("codigoIata")), patron));
        };
    }

    public static Specification<Vuelo> destinoContiene(String destino) {
        return (root, query, cb) -> {
            if (destino == null) {
                return null;
            }
            String patron = "%" + destino.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("destino").get("ciudad")), patron),
                    cb.like(cb.lower(root.get("destino").get("codigoIata")), patron));
        };
    }

    public static Specification<Vuelo> deCategoria(Long categoriaId) {
        return (root, query, cb) -> categoriaId == null ? null
                : cb.equal(root.get("categoria").get("id"), categoriaId);
    }

    public static Specification<Vuelo> delVendedor(Long vendedorId) {
        return (root, query, cb) -> vendedorId == null ? null
                : cb.equal(root.get("vendedor").get("id"), vendedorId);
    }

    public static Specification<Vuelo> esClase(ClaseVuelo clase) {
        return (root, query, cb) -> clase == null ? null : cb.equal(root.get("clase"), clase);
    }

    public static Specification<Vuelo> precioMinimo(BigDecimal precioMin) {
        return (root, query, cb) -> precioMin == null ? null
                : cb.greaterThanOrEqualTo(root.get("precio"), precioMin);
    }

    public static Specification<Vuelo> precioMaximo(BigDecimal precioMax) {
        return (root, query, cb) -> precioMax == null ? null
                : cb.lessThanOrEqualTo(root.get("precio"), precioMax);
    }
}
