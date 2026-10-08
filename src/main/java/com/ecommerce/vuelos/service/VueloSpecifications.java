package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.entity.EstadoVuelo;
import com.ecommerce.vuelos.entity.Vuelo;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class VueloSpecifications {

    private VueloSpecifications() {
    }

    /** Un vuelo dado de baja no se lista nunca. */
    public static Specification<Vuelo> soloPublicados() {
        return (root, query, cb) -> cb.notEqual(root.get("estado"), EstadoVuelo.ELIMINADO);
    }

    /** Un vuelo que ya salio no se puede comprar, asi que tampoco se lista. */
    public static Specification<Vuelo> todaviaNoSalio() {
        return (root, query, cb) -> cb.greaterThan(root.get("fechaSalida"), LocalDateTime.now());
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

    /** Filtra por clase navegando hacia las disponibilidades del vuelo. */
    public static Specification<Vuelo> deClase(Long claseId) {
        return (root, query, cb) -> {
            if (claseId == null) {
                return null;
            }
            query.distinct(true);
            return cb.equal(root.join("disponibilidades").get("clase").get("id"), claseId);
        };
    }

    public static Specification<Vuelo> precioMinimo(BigDecimal precioMin) {
        return (root, query, cb) -> precioMin == null ? null
                : cb.greaterThanOrEqualTo(root.get("precio"), precioMin);
    }

    /** Vuelos que salen ese dia o despues (para el calendario de la busqueda). */
    public static Specification<Vuelo> saleDesde(LocalDate fechaDesde) {
        return (root, query, cb) -> fechaDesde == null ? null
                : cb.greaterThanOrEqualTo(root.get("fechaSalida"), fechaDesde.atStartOfDay());
    }

    /** Vuelos que salen ese dia o antes. */
    public static Specification<Vuelo> saleHasta(LocalDate fechaHasta) {
        return (root, query, cb) -> fechaHasta == null ? null
                : cb.lessThan(root.get("fechaSalida"), fechaHasta.plusDays(1).atStartOfDay());
    }

    public static Specification<Vuelo> precioMaximo(BigDecimal precioMax) {
        return (root, query, cb) -> precioMax == null ? null
                : cb.lessThanOrEqualTo(root.get("precio"), precioMax);
    }
}
