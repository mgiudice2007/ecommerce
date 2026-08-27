package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByPasajeroIdOrderByFechaDesc(Long pasajeroId);
}
