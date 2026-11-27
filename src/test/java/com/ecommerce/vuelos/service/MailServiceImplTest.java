package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.embarque.EmbarqueResponse;
import com.ecommerce.vuelos.dto.orden.ItemOrdenResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.dto.orden.PasajeResponse;
import com.ecommerce.vuelos.entity.EstadoEmbarque;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.MedioPago;
import com.ecommerce.vuelos.entity.TipoDocumento;
import com.ecommerce.vuelos.entity.TipoPasajero;
import com.ecommerce.vuelos.event.OrdenConfirmadaEvent;
import com.ecommerce.vuelos.event.PasajeroEmbarcadoEvent;
import com.ecommerce.vuelos.event.VueloModificadoEvent;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private QrService qrService;

    @Mock
    private OrdenPdfService ordenPdfService;

    @InjectMocks
    private MailServiceImpl mailService;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(mailService, "habilitado", true);
        ReflectionTestUtils.setField(mailService, "remitente", "no-reply@bcaairlines.com");
        ReflectionTestUtils.setField(mailService, "horasParaCancelar", 24);
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

    /** Recorre las partes del mail: devuelve el texto HTML, las imagenes del cuerpo y los adjuntos. */
    private void recorrer(Part parte, StringBuilder html, List<String> imagenes, List<String> adjuntos) throws Exception {
        if (parte.isMimeType("multipart/*")) {
            Multipart partes = (Multipart) parte.getContent();
            for (int i = 0; i < partes.getCount(); i++) {
                BodyPart hija = partes.getBodyPart(i);
                recorrer(hija, html, imagenes, adjuntos);
            }
        } else if (parte.isMimeType("text/html")) {
            html.append((String) parte.getContent());
        } else if (parte.isMimeType("image/*")) {
            imagenes.add(((jakarta.mail.internet.MimeBodyPart) parte).getContentID());
        } else if (parte.getFileName() != null) {
            adjuntos.add(parte.getFileName());
        }
    }

    private MimeMessage enviar() throws Exception {
        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(enviado.capture());
        MimeMessage mail = enviado.getValue();
        mail.saveChanges();
        return mail;
    }

    @Test
    void armaYEnviaElMailConElDetalleYLosPasesDeEmbarque() throws Exception {
        when(qrService.png(anyString(), anyInt())).thenReturn(new byte[] {1, 2, 3});
        when(ordenPdfService.ordenDeCompra(any(), anyString(), anyString())).thenReturn(new byte[] {4, 5, 6});
        MimeMessage mensaje = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mensaje);

        mailService.enviarConfirmacionCompra(evento());

        MimeMessage mail = enviar();
        assertEquals("ana@test.com", mail.getRecipients(Message.RecipientType.TO)[0].toString());
        assertTrue(mail.getSubject().contains("#77"));

        StringBuilder html = new StringBuilder();
        List<String> imagenes = new ArrayList<>();
        List<String> adjuntos = new ArrayList<>();
        recorrer(mail, html, imagenes, adjuntos);
        String cuerpo = html.toString();
        assertTrue(cuerpo.contains("cid:logo"));
        assertTrue(cuerpo.contains("cid:banner"));
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
        // logo + banner + un QR por pasajero
        assertEquals(4, imagenes.size());
        verify(qrService).png("BCA-AAAA2222", 120);
        verify(qrService).png("BCA-BBBB3333", 120);

        // la orden de compra viaja adjunta en PDF
        assertEquals(List.of("orden-de-compra-77.pdf"), adjuntos);
        assertTrue(cuerpo.contains("orden de compra en PDF"));
        verify(ordenPdfService).ordenDeCompra(any(), eq("Ana <b>"), eq("ana@test.com"));
    }

    @Test
    void siElPdfFalla_elMailSaleIgualSinAdjunto() throws Exception {
        when(qrService.png(anyString(), anyInt())).thenReturn(new byte[] {1});
        when(ordenPdfService.ordenDeCompra(any(), anyString(), anyString())).thenThrow(new IllegalStateException("sin logo"));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        mailService.enviarConfirmacionCompra(evento());

        MimeMessage mail = enviar();
        StringBuilder html = new StringBuilder();
        List<String> adjuntos = new ArrayList<>();
        recorrer(mail, html, new ArrayList<>(), adjuntos);
        assertTrue(adjuntos.isEmpty());
        assertTrue(!html.toString().contains("orden de compra en PDF"));
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

    private PasajeroEmbarcadoEvent embarco() {
        EmbarqueResponse embarque = EmbarqueResponse.builder()
                .codigoEmbarque("BCA-AAAA2222").estado(EstadoEmbarque.EMBARCADO).nombre("Luis").apellido("Gomez <i>")
                .documento("30111222").ordenId(77L).numeroVuelo("BCA-101").origen("Buenos Aires").destino("Miami")
                .fechaSalida(LocalDateTime.of(2027, 1, 10, 8, 30)).claseNombre("Económica").asiento("12B")
                .embarcadoEn(LocalDateTime.of(2027, 1, 10, 8, 5))
                .build();
        return new PasajeroEmbarcadoEvent("ana@test.com", "Ana", embarque);
    }

    @Test
    void alEmbarcar_leMandaAlCompradorElMailDeBuenViaje() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        mailService.enviarBuenViaje(embarco());

        MimeMessage mail = enviar();
        assertEquals("ana@test.com", mail.getRecipients(Message.RecipientType.TO)[0].toString());
        assertTrue(mail.getSubject().contains("Buen viaje, Luis"));
        assertTrue(mail.getSubject().contains("BCA-101"));

        StringBuilder html = new StringBuilder();
        List<String> imagenes = new ArrayList<>();
        recorrer(mail, html, imagenes, new ArrayList<>());
        String cuerpo = html.toString();
        assertTrue(cuerpo.contains("Hola Ana"));
        assertTrue(cuerpo.contains("se validó el pase de embarque"));
        // el apellido se escapa: no puede meter HTML en el mail
        assertTrue(cuerpo.contains("Luis Gomez &lt;i&gt;"));
        assertTrue(cuerpo.contains("Asiento <strong>12B</strong>"));
        assertTrue(cuerpo.contains("10/01/2027 08:05"));
        assertTrue(cuerpo.contains("excelente viaje a <strong>Miami</strong>"));
        // logo + banner de buen viaje, sin QR ni adjuntos
        assertEquals(2, imagenes.size());
        verify(qrService, never()).png(anyString(), anyInt());
    }

    @Test
    void siElSmtpFallaAlEmbarcar_noPropagaLaExcepcion() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP caido")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> mailService.enviarBuenViaje(embarco()));
    }

    @Test
    void conElMailDeshabilitado_noMandaElDeBuenViaje() {
        ReflectionTestUtils.setField(mailService, "habilitado", false);

        mailService.enviarBuenViaje(embarco());

        verify(mailSender, never()).createMimeMessage();
    }

    private VueloModificadoEvent aviso(VueloModificadoEvent.Motivo motivo) {
        LocalDateTime salida = LocalDateTime.of(2027, 1, 10, 13, 30);
        boolean reprogramado = motivo == VueloModificadoEvent.Motivo.REPROGRAMADO;
        return new VueloModificadoEvent(motivo, "BCA-101", "Buenos Aires", "Miami", salida, salida.plusHours(9),
                reprogramado ? LocalDateTime.of(2027, 1, 10, 8, 30) : null,
                reprogramado ? LocalDateTime.of(2027, 1, 10, 17, 30) : null,
                List.of(new VueloModificadoEvent.Comprador("ana@test.com", "Ana <b>"),
                        new VueloModificadoEvent.Comprador("luis@test.com", "Luis")));
    }

    private String cuerpoDe(MimeMessage mail, List<String> imagenes) throws Exception {
        StringBuilder html = new StringBuilder();
        recorrer(mail, html, imagenes, new ArrayList<>());
        return html.toString();
    }

    @Test
    void alReprogramar_leMandaUnMailACadaCompradorConElHorarioViejoYElNuevo() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer((invocacion) -> new MimeMessage((Session) null));

        mailService.enviarAvisoVuelo(aviso(VueloModificadoEvent.Motivo.REPROGRAMADO));

        ArgumentCaptor<MimeMessage> enviados = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(2)).send(enviados.capture());
        MimeMessage mail = enviados.getAllValues().get(0);
        mail.saveChanges();
        assertEquals("ana@test.com", mail.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("luis@test.com", enviados.getAllValues().get(1).getRecipients(Message.RecipientType.TO)[0].toString());
        assertTrue(mail.getSubject().contains("Tu vuelo BCA-101 cambió de horario"));

        List<String> imagenes = new ArrayList<>();
        String cuerpo = cuerpoDe(mail, imagenes);
        // el nombre se escapa: no puede meter HTML en el mail
        assertTrue(cuerpo.contains("Hola Ana &lt;b&gt;"));
        assertTrue(cuerpo.contains("NUEVO HORARIO"));
        assertTrue(cuerpo.contains("10/01/2027 08:30"));
        assertTrue(cuerpo.contains("10/01/2027 13:30"));
        assertTrue(cuerpo.contains("line-through"));
        assertTrue(cuerpo.contains("hasta 24 horas antes"));
        // logo + banner del aviso
        assertEquals(2, imagenes.size());
    }

    @Test
    void alCancelarElVuelo_avisaQueSePuedeCancelarLaCompraSinLimite() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer((invocacion) -> new MimeMessage((Session) null));

        mailService.enviarAvisoVuelo(aviso(VueloModificadoEvent.Motivo.CANCELADO));

        ArgumentCaptor<MimeMessage> enviados = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(2)).send(enviados.capture());
        MimeMessage mail = enviados.getAllValues().get(0);
        mail.saveChanges();
        assertTrue(mail.getSubject().contains("Tu vuelo BCA-101 fue cancelado"));
        String cuerpo = cuerpoDe(mail, new ArrayList<>());
        assertTrue(cuerpo.contains("VUELO CANCELADO"));
        assertTrue(cuerpo.contains("cuando quieras, aunque falten menos de 24 horas"));
        assertTrue(!cuerpo.contains("line-through"));
    }

    @Test
    void siFallaElMailDeUnComprador_seSigueConLosDemas() {
        when(mailSender.createMimeMessage()).thenAnswer((invocacion) -> new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP caido")).doNothing().when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> mailService.enviarAvisoVuelo(aviso(VueloModificadoEvent.Motivo.DEMORADO)));

        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void conElMailDeshabilitado_noMandaElAvisoDeVuelo() {
        ReflectionTestUtils.setField(mailService, "habilitado", false);

        mailService.enviarAvisoVuelo(aviso(VueloModificadoEvent.Motivo.DEMORADO));

        verify(mailSender, never()).createMimeMessage();
    }
}
