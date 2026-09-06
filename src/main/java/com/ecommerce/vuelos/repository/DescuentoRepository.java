package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Descuento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DescuentoRepository extends JpaRepository<Descuento, Long> {

    List<Descuento> findByVueloId(Long vueloId); //todos los descuentos de un vuelo (vigentes o no, para el historial)

    List<Descuento> findByVueloIdAndActivoTrue(Long vueloId); //solo los que están prendidos
}
