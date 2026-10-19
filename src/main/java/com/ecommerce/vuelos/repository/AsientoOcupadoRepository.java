package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.AsientoOcupado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsientoOcupadoRepository extends JpaRepository<AsientoOcupado, Long> {

    List<AsientoOcupado> findByDisponibilidadId(Long disponibilidadId); //los asientos vendidos de un vuelo y clase
}
