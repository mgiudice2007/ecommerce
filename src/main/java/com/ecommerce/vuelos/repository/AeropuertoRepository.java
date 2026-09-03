package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Aeropuerto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AeropuertoRepository extends JpaRepository<Aeropuerto, String> {

    List<Aeropuerto> findByCiudadIgnoreCase(String ciudad);

    List<Aeropuerto> findByPaisIgnoreCase(String pais);
}
