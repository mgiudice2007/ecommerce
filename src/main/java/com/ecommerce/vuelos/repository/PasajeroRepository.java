package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Pasajero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasajeroRepository extends JpaRepository<Pasajero, Long> {

    Optional<Pasajero> findByUsername(String username);
}
