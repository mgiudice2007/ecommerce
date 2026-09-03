package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.model.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
    boolean existsByVueloId(Long vueloId);
}
