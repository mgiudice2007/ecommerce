package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Aerolinea;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AerolineaRepository extends JpaRepository<Aerolinea, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}
