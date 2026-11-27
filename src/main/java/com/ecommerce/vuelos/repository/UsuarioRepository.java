package com.ecommerce.vuelos.repository;

import com.ecommerce.vuelos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsernameAndMillasIsNull(String username);

    boolean existsByUsername(String username);

    boolean existsByMail(String mail);

    boolean existsByDni(String dni);

    // Los que compraron pasajes en el vuelo y no cancelaron la compra, una vez cada uno aunque tengan varias
    @Query("select distinct o.usuario from Orden o join o.items i "
            + "where i.disponibilidad.vuelo.id = ?1 and o.estado = com.ecommerce.vuelos.entity.EstadoOrden.CONFIRMADA")
    List<Usuario> compradoresDelVuelo(Long vueloId);

    /**
     * Las cuentas viejas que tenian el rol VENDEDOR (ya no existe) pasan a COMPRADOR.
     * Va en SQL directo porque el enum Rol ya no tiene VENDEDOR y JPA no podria leer esas filas.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE usuarios SET rol = 'COMPRADOR' WHERE rol = 'VENDEDOR'", nativeQuery = true)
    int pasarVendedoresAComprador();
}
