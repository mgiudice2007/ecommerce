package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.ItemOrden;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemOrdenRepository extends JpaRepository<ItemOrden, Long> {
    boolean existsByVueloId(Long vueloId);
}
