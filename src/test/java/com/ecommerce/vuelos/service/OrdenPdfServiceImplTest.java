package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.ItemOrdenResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.dto.orden.PasajeResponse;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.MedioPago;
import com.ecommerce.vuelos.entity.TipoDocumento;
import com.ecommerce.vuelos.entity.TipoPasajero;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenPdfServiceImplTest {

    private final OrdenPdfServiceImpl pdfService = new OrdenPdfServiceImpl();

    private OrdenResponse orden(List<PasajeResponse> pasajes) {
        ItemOrdenResponse item = ItemOrdenResponse.builder()
                .id(1L).numeroVuelo("BCA-101").origen("Buenos Aires").destino("Miami")
                .fechaSalida(LocalDateTime.of(2027, 1, 10, 8, 30))
                .claseNombre("Económica").tipoPasajero(TipoPasajero.ADULTO).cantidad(pasajes.size())
                .pasajes(pasajes).precioUnitario(new BigDecimal("1000")).subtotal(new BigDecimal("1000"))
                .build();
        return OrdenResponse.builder()
                .id(77L).total(new BigDecimal("1000")).millasGanadas(4).millasUsadas(0)
                .descuentoMillas(BigDecimal.ZERO).fecha(LocalDateTime.of(2026, 11, 16, 12, 0))
                .estado(EstadoOrden.CONFIRMADA).medioPago(MedioPago.TARJETA_DEBITO).ultimosDigitos("1111")
                .items(List.of(item)).cancelableHasta(LocalDateTime.of(2027, 1, 9, 8, 30))
                .build();
    }

    @Test
    void armaUnPdfConLaOrdenYSusPasajeros() {
        PasajeResponse pasaje = PasajeResponse.builder().id(1L).nombre("Ana").apellido("Gómez")
                .tipoDocumento(TipoDocumento.DNI).documento("30111222").asiento("12A").codigoEmbarque("BCA-AAAA2222").build();

        byte[] pdf = pdfService.ordenDeCompra(orden(List.of(pasaje)), "Ana Gómez", "ana@test.com");

        // Todo PDF empieza con la firma %PDF
        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).equals("%PDF"));
        assertTrue(pdf.length > 1000);
    }

    @Test
    void sinPasajesTambienSeArma() {
        byte[] pdf = pdfService.ordenDeCompra(orden(List.of()), "Ana", "ana@test.com");

        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).equals("%PDF"));
    }
}
