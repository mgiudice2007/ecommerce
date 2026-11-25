package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.ItemOrdenResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.dto.orden.PasajeResponse;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.MedioPago;
import com.ecommerce.vuelos.entity.TipoDocumento;
import com.ecommerce.vuelos.entity.TipoPasajero;
import com.ecommerce.vuelos.event.OrdenConfirmadaEvent;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private QrService qrService;

    @InjectMocks
    private MailServiceImpl mailService;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(mailService, "habilitado", true);
        ReflectionTestUtils.setField(mailService, "remitente", "no-reply@bcaairlines.com");
    }

    private PasajeResponse pasaje(String nombre, String asiento, String codigo) {
        return PasajeResponse.builder().id(1L).nombre(nombre).apellido("Gómez").tipoDocumento(TipoDocumento.DNI)
                .documento("30111222").fechaNacimiento(LocalDate.of(1990, 1, 1)).asiento(asiento)
                .codigoEmbarque(codigo).build();
    }

    private OrdenConfirmadaEvent evento() {
        ItemOrdenResponse item = ItemOrdenResponse.builder()
                .id(1L).numeroVuelo("BCA-101").origen("Buenos Aires").destino("Miami")
                .fechaSalida(LocalDateTime.of(2027, 1, 10, 8, 30))
                .claseNombre("Económica").tipoPasajero(TipoPasajero.ADULTO).cantidad(2)
                .asientos(List.of("12A", "12B"))
                .pasajes(List.of(pasaje("Ana", "12A", "BCA-AAAA2222"), pasaje("Luis", "12B", "BCA-BBBB3333")))
                .precioUnitario(new BigDecimal("1000")).subtotal(new BigDecimal("2000"))
                .build();
        OrdenResponse orden = OrdenResponse.builder()
                .id(77L).total(new BigDecimal("2000")).descuentoTotal(BigDecimal.ZERO)
                .millasGanadas(8).millasUsadas(0).descuentoMillas(BigDecimal.ZERO)
                .fecha(LocalDateTime.of(2026, 11, 16, 12, 0)).estado(EstadoOrden.CONFIRMADA)
                .medioPago(MedioPago.TARJETA_CREDITO).ultimosDigitos("4242")
                .items(List.of(item)).cancelableHasta(LocalDateTime.of(2027, 1, 9, 8, 30)).sePuedeCancelar(true)
                .build();
        return new OrdenConfirmadaEvent("ana@test.com", "Ana <b>", orden);
    }

    /** Recorre las partes del mail: devuelve el texto HTML y cuenta las imagenes adjuntas. */
    private void recorrer(Part parte, StringBuilder html, List<String> imagenes) throws Exception {
        if (parte.isMimeType("multipart/*")) {
            Multipart partes = (Multipart) parte.getContent();
            for (int i = 0; i < partes.getCount(); i++) {
                BodyPart hija = partes.getBodyPart(i);
                recorrer(hija, html, imagenes);
            }
        } else if (parte.isMimeType("text/html")) {
            html.append((String) parte.getContent());
        } else if (parte.isMimeType("image/png")) {
            imagenes.add(((jakarta.mail.internet.MimeBodyPart) parte).getContentID());
        }
    }

    @Test
    void armaYEnviaElMailConElDetalleYLosPasesDeEmbarque() throws Exception {
        when(qrService.png(anyString(), anyInt())).thenReturn(new byte[] {1, 2, 3});
        MimeMessage mensaje = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mensaje);

        mailService.enviarConfirmacionCompra(evento());

        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(enviado.capture());
        MimeMessage mail = enviado.getValue();
        mail.saveChanges();
        assertEquals("ana@test.com", mail.getRecipients(Message.RecipientType.TO)[0].toString());
        assertTrue(mail.getSubject().contains("#77"));

        StringBuilder html = new StringBuilder();
        List<String> imagenes = new ArrayList<>();
        recorrer(mail, html, imagenes);
        String cuerpo = html.toString();
        assertTrue(cuerpo.contains("Buenos Aires"));
        assertTrue(cuerpo.contains("BCA-101"));
        assertTrue(cuerpo.contains("12A, 12B"));
        assertTrue(cuerpo.contains("terminada en 4242"));
        // el nombre del usuario se escapa: no puede meter HTML en el mail
        assertTrue(cuerpo.contains("Ana &lt;b&gt;"));

        // un pase por pasajero, cada uno con su codigo y su QR como imagen adjunta
        assertTrue(cuerpo.contains("Tus pases de embarque"));
        assertTrue(cuerpo.contains("BCA-AAAA2222"));
        assertTrue(cuerpo.contains("cid:qr-BCA-BBBB3333"));
        assertEquals(2, imagenes.size());
        verify(qrService).png("BCA-AAAA2222", 120);
        verify(qrService).png("BCA-BBBB3333", 120);
    }

    @Test
    void siElSmtpFalla_noPropagaLaExcepcion() {
        when(qrService.png(anyString(), anyInt())).thenReturn(new byte[] {1});
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP caido")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> mailService.enviarConfirmacionCompra(evento()));
    }

    @Test
    void conElMailDeshabilitado_noMandaNada() {
        ReflectionTestUtils.setField(mailService, "habilitado", false);

        mailService.enviarConfirmacionCompra(evento());

        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(mailSender, never()).createMimeMessage();
    }
}
