package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

    List<Disponibilidad> findByVueloId(Long vueloId);

    Optional<Disponibilidad> findByVueloIdAndClaseId(Long vueloId, Long claseId);

    boolean existsByVueloIdAndClaseId(Long vueloId, Long claseId);
}
