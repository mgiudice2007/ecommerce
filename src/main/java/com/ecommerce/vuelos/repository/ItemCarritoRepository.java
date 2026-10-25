package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
    boolean existsByDisponibilidadId(Long disponibilidadId);

    List<ItemCarrito> findByDisponibilidadId(Long disponibilidadId); //los carritos que tienen esa clase de ese vuelo
}
