package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Pasaje;
import com.ecommerce.vuelos.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasajeRepository extends JpaRepository<Pasaje, Long> {

    Optional<Pasaje> findByCodigoEmbarque(String codigoEmbarque);

    boolean existsByCodigoEmbarque(String codigoEmbarque);

    // Si la persona ya tiene un pasaje en ese vuelo, en una compra que no se cancelo
    @Query("select count(p) > 0 from Pasaje p "
            + "where p.itemOrden.disponibilidad.vuelo.id = ?1 and p.tipoDocumento = ?2 and p.documento = ?3 "
            + "and p.itemOrden.orden.estado <> com.ecommerce.vuelos.entity.EstadoOrden.CANCELADA")
    boolean existeEnVuelo(Long vueloId, TipoDocumento tipoDocumento, String documento);
}
