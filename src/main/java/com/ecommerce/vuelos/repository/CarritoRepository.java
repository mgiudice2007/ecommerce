package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByPasajeroId(Long pasajeroId);
}
