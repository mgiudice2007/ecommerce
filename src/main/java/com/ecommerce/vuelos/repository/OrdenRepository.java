package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Orden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    List<Orden> findByPasajeroIdOrderByFechaDesc(Long pasajeroId);
}
