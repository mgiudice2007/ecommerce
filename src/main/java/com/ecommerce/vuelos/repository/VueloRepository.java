package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.model.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VueloRepository extends JpaRepository<Vuelo, Long>, JpaSpecificationExecutor<Vuelo> {

    boolean existsByAerolineaId(Long aerolineaId);
}
