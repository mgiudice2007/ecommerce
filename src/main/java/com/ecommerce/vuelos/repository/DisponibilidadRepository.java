package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

    List<Disponibilidad> findByVueloId(Long vueloId); //todos los cupos de un vuelo

    Optional<Disponibilidad> findByVueloIdAndClaseId(Long vueloId, Long claseId); //el cupo de una clase puntual

    boolean existsByVueloIdAndClaseId(Long vueloId, Long claseId);

    // Totales para el panel del admin (vuelos publicados que todavia no salieron)
    @Query("select coalesce(sum(d.asientosTotales), 0) from Disponibilidad d "
            + "where d.vuelo.estado <> com.ecommerce.vuelos.entity.EstadoVuelo.ELIMINADO and d.vuelo.fechaSalida > :ahora")
    long sumarAsientosTotales(@Param("ahora") LocalDateTime ahora);

    @Query("select coalesce(sum(d.asientosTotales - d.asientosDisponibles), 0) from Disponibilidad d "
            + "where d.vuelo.estado <> com.ecommerce.vuelos.entity.EstadoVuelo.ELIMINADO and d.vuelo.fechaSalida > :ahora")
    long sumarAsientosVendidos(@Param("ahora") LocalDateTime ahora); //¿ya existe ese cupo? (para no duplicar)
}
