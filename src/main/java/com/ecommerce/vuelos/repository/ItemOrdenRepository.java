package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.ItemOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemOrdenRepository extends JpaRepository<ItemOrden, Long> {
    boolean existsByDisponibilidadId(Long disponibilidadId);
}
