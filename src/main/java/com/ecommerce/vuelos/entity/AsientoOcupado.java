package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un asiento vendido (por ejemplo "12A") de un vuelo y clase. La restriccion unica
 * asegura que el mismo asiento no se pueda vender dos veces. Al cancelar la compra
 * se borra y el asiento vuelve a quedar libre.
 */
@Entity
@Table(name = "asientos_ocupados",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_asiento_disponibilidad_codigo",
                columnNames = {"disponibilidad_id", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsientoOcupado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disponibilidad_id", nullable = false)
    private Disponibilidad disponibilidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_orden_id", nullable = false)
    private ItemOrden itemOrden;

    @Column(nullable = false, length = 5)
    private String codigo;
}
