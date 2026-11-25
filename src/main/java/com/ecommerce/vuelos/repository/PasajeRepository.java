package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Pasaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasajeRepository extends JpaRepository<Pasaje, Long> {

    Optional<Pasaje> findByCodigoEmbarque(String codigoEmbarque);

    boolean existsByCodigoEmbarque(String codigoEmbarque);
}
