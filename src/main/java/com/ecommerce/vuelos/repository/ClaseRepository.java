package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Clase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClaseRepository extends JpaRepository<Clase, Long> {

    Optional<Clase> findByNombre(String nombre);
}
