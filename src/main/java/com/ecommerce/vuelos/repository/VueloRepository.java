package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

@Repository
public interface VueloRepository extends JpaRepository<Vuelo, Long>, JpaSpecificationExecutor<Vuelo> {

    boolean existsByNumeroVuelo(String numeroVuelo);

    Optional<Vuelo> findFirstByNumeroVuelo(String numeroVuelo);
}
