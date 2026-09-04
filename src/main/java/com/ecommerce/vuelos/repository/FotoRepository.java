package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Foto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FotoRepository extends JpaRepository<Foto, Long> {

    List<Foto> findByVueloIdOrderByOrdenAsc(Long vueloId);

    long countByVueloId(Long vueloId);
}
