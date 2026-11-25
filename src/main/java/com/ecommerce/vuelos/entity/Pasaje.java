package com.ecommerce.vuelos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un pasaje es una persona en un vuelo: nombre, documento, asiento y el codigo de embarque
 * que lleva el QR. El asiento se guarda como texto (no como relacion) para que el pasaje
 * siga mostrandolo aunque la compra se cancele y el asiento se libere.
 */
@Entity
@Table(name = "pasajes", uniqueConstraints = @UniqueConstraint(name = "uk_pasaje_codigo", columnNames = "codigo_embarque"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pasaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_orden_id", nullable = false)
    private ItemOrden itemOrden;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String apellido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 12)
    private String documento;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    // Los bebes viajan a upa: no tienen asiento propio
    @Column(length = 5)
    private String asiento;

    @Column(name = "codigo_embarque", nullable = false, length = 12)
    private String codigoEmbarque;

    // Se completa cuando el pasajero embarca (vista "Validar embarque" del administrador)
    @Column
    private LocalDateTime embarcadoEn;
}
