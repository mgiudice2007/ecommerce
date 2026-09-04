package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Query("select c from Categoria c where c.nombre = ?1")
    List<Categoria> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
