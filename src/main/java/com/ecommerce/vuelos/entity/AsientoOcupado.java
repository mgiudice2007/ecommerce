package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


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
