package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface VueloRepository extends JpaRepository<Vuelo, Long>, JpaSpecificationExecutor<Vuelo> {
}
