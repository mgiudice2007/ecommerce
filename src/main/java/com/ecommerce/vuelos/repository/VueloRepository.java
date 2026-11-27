package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VueloRepository extends JpaRepository<Vuelo, Long>, JpaSpecificationExecutor<Vuelo> {

    boolean existsByNumeroVuelo(String numeroVuelo);

    Optional<Vuelo> findFirstByNumeroVuelo(String numeroVuelo);

    // Otro vuelo con el mismo numero que sale ese dia (sin contar los eliminados ni el que se esta editando)
    @Query("select count(v) > 0 from Vuelo v where v.numeroVuelo = ?1 and v.fechaSalida >= ?2 and v.fechaSalida < ?3 "
            + "and v.estado <> com.ecommerce.vuelos.entity.EstadoVuelo.ELIMINADO and v.id <> ?4")
    boolean existeOtroEseDia(String numeroVuelo, LocalDateTime desde, LocalDateTime hasta, Long vueloId);
}
