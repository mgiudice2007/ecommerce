package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
    boolean existsByDisponibilidadId(Long disponibilidadId);
}
