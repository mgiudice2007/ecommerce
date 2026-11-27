package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.ItemOrdenResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.dto.orden.PasajeResponse;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.MedioPago;
import com.ecommerce.vuelos.entity.TipoDocumento;
import com.ecommerce.vuelos.entity.TipoPasajero;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private ItemOrdenResponse tramo(long vueloId, String numero, String origen, String destino, LocalDateTime salida,
                                    TipoPasajero tipo, PasajeResponse pasaje) {
        return ItemOrdenResponse.builder()
                .id(vueloId).vueloId(vueloId).numeroVuelo(numero).origen(origen).destino(destino)
                .fechaSalida(salida).fechaLlegada(salida.plusHours(9)).duracionMinutos(540)
                .claseNombre("Económica").tipoPasajero(tipo).cantidad(1).pasajes(List.of(pasaje))
                .precioUnitario(new BigDecimal("1000")).subtotal(new BigDecimal("1000"))
                .build();
    }

    private String texto(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        StringBuilder texto = new StringBuilder();
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(new PdfTextExtractor(lector).getTextFromPage(pagina)).append(System.lineSeparator());
        }
        return texto.toString();
    }

    @Test
    void idaYVuelta_agrupaLosPasajerosPorVueloYMuestraElPago() throws IOException {
        PasajeResponse ana = PasajeResponse.builder().nombre("Ana").apellido("Gómez").tipoDocumento(TipoDocumento.DNI)
                .documento("30111222").asiento("12A").codigoEmbarque("BCA-IDA1").build();
        PasajeResponse bebe = PasajeResponse.builder().nombre("Sofía").apellido("Gómez").tipoDocumento(TipoDocumento.DNI)
                .documento("55123456").codigoEmbarque("BCA-IDA2").build();
        PasajeResponse anaVuelta = PasajeResponse.builder().nombre("Ana").apellido("Gómez").tipoDocumento(TipoDocumento.DNI)
                .documento("30111222").asiento("21C").codigoEmbarque("BCA-VUE1").build();
        LocalDateTime ida = LocalDateTime.of(2027, 1, 10, 8, 30);
        OrdenResponse orden = OrdenResponse.builder()
                .id(1042L).total(new BigDecimal("2000")).millasGanadas(2000).millasUsadas(0)
                .descuentoMillas(BigDecimal.ZERO).fecha(LocalDateTime.of(2026, 11, 27, 16, 5))
                .estado(EstadoOrden.CONFIRMADA).medioPago(MedioPago.TARJETA_CREDITO).ultimosDigitos("4242")
                .items(List.of(
                        tramo(1, "BCA-101", "Buenos Aires", "Miami", ida, TipoPasajero.ADULTO, ana),
                        tramo(1, "BCA-101", "Buenos Aires", "Miami", ida, TipoPasajero.BEBE, bebe),
                        tramo(2, "BCA-202", "Miami", "Buenos Aires", ida.plusDays(14), TipoPasajero.ADULTO, anaVuelta)))
                .cancelableHasta(ida.minusHours(24))
                .build();

        String texto = texto(pdfService.ordenDeCompra(orden, "Ana Gómez", "ana@test.com"));

        assertTrue(texto.contains("IDA"));
        assertTrue(texto.contains("VUELTA"));
        assertTrue(texto.contains("1 adulto · 1 bebé"), "el adulto y el bebe del mismo vuelo van en una tarjeta");
        assertTrue(texto.contains("GÓMEZ, Ana"));
        assertTrue(texto.contains("En brazos"));
        assertTrue(texto.contains("Domingo 10 de enero de 2027"));
        assertTrue(texto.contains("Total pagado"));
        assertTrue(texto.contains("Sumaste 2.000 millas"));
        assertTrue(texto.contains("Página 1 de"));
        assertFalse(texto.contains("Compra cancelada"));
    }
}
