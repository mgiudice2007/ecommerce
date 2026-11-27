package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.embarque.EmbarqueResponse;
import com.ecommerce.vuelos.dto.orden.ItemOrdenResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.dto.orden.PasajeResponse;
import com.ecommerce.vuelos.event.OrdenConfirmadaEvent;
import com.ecommerce.vuelos.event.PasajeroEmbarcadoEvent;
import com.ecommerce.vuelos.event.VueloModificadoEvent;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    private static final int LADO_QR = 120;

    // Los mails no cargan hojas de estilo: todo el diseño va en estilos en linea y con tablas
    private static final String AZUL = "#0057b8";
    private static final String CELESTE = "#e8f1f9";
    private static final String GRIS = "#5b6b7b";
    private static final String BORDE = "#dde3ea";
    private static final String VERDE = "#1f8a4c";
    private static final String NARANJA = "#c2410c";
    private static final String ROJO = "#b42318";
    private static final String FUENTE = "font-family:Arial,Helvetica,sans-serif;";

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private QrService qrService;
    @Autowired
    private OrdenPdfService ordenPdfService;

    // Con mail.habilitado=false (por ejemplo en una demo sin SMTP) no se intenta mandar nada
    @Value("${mail.habilitado}")
    private boolean habilitado;
    @Value("${mail.remitente}")
    private String remitente;
    // Hasta cuantas horas antes del vuelo se puede cancelar, para el texto del aviso de vuelo
    @Value("${ordenes.horas-para-cancelar}")
    private int horasParaCancelar;

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarConfirmacionCompra(OrdenConfirmadaEvent evento) {
        if (!habilitado) {
            return;
        }
        OrdenResponse orden = evento.orden();
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "BCA Airlines");
            helper.setTo(evento.destinatario());
            helper.setSubject("BCA Airlines · Confirmación de tu compra #" + orden.getId());
            byte[] pdf = armarPdf(evento);
            // El logo, el banner y los QR viajan como imagenes dentro del mail (cid:): los clientes de
            // mail no muestran SVG y muchos bloquean las imagenes que se cargan desde afuera
            Map<String, byte[]> codigosQr = new LinkedHashMap<>();
            helper.setText(armarCuerpo(evento, codigosQr, pdf != null), true);
            helper.addInline("logo", new ClassPathResource("mail/logo.png"), "image/png");
            helper.addInline("banner", new ClassPathResource("mail/banner.jpg"), "image/jpeg");
            for (Map.Entry<String, byte[]> qr : codigosQr.entrySet()) {
                helper.addInline(qr.getKey(), new ByteArrayResource(qr.getValue()), "image/png");
            }
            if (pdf != null) {
                helper.addAttachment("orden-de-compra-" + orden.getId() + ".pdf", new ByteArrayResource(pdf), "application/pdf");
            }
            mailSender.send(mensaje);
            log.info("Mail de confirmacion de la compra #{} enviado", orden.getId());
        } catch (Exception e) {
            // El pago ya esta confirmado: que falle el mail no puede deshacer la compra
            log.warn("No se pudo enviar el mail de la compra #{}: {}", orden.getId(), e.getMessage());
        }
    }

    /** Si el PDF falla, el mail sale igual sin el adjunto. */
    private byte[] armarPdf(OrdenConfirmadaEvent evento) {
        try {
            return ordenPdfService.ordenDeCompra(evento.orden(), evento.nombre(), evento.destinatario());
        } catch (Exception e) {
            log.warn("No se pudo armar el PDF de la compra #{}: {}", evento.orden().getId(), e.getMessage());
            return null;
        }
    }

    private String armarCuerpo(OrdenConfirmadaEvent evento, Map<String, byte[]> codigosQr, boolean conPdf) {
        OrdenResponse orden = evento.orden();
        StringBuilder html = new StringBuilder();
        abrirMail(html, "Tu compra #" + orden.getId() + " está confirmada. Acá tenés tus pases de embarque.",
                "Compra confirmada. ¡Tu viaje está en marcha!");
        html.append("<p style=\"font-size:19px;margin:0 0 18px\"><strong>Hola ").append(escapar(evento.nombre()))
                .append(", ¡tu compra está confirmada!</strong></p>");
        agregarResumen(html, orden);

        html.append(titulo("Tu viaje"));
        html.append("<table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse\">");
        for (ItemOrdenResponse item : orden.getItems()) {
            html.append("<tr><td style=\"padding:12px 0;border-bottom:1px solid ").append(BORDE).append("\">");
            html.append("<strong style=\"font-size:15px\">").append(escapar(item.getOrigen())).append(" → ")
                    .append(escapar(item.getDestino())).append("</strong><br>");
            html.append("<span style=\"color:").append(GRIS).append(";font-size:13px;line-height:20px\">Vuelo ")
                    .append(escapar(item.getNumeroVuelo()));
            if (item.getFechaSalida() != null) {
                html.append("<br>").append(TextoOrden.horario(item.getFechaSalida(), item.getFechaLlegada(),
                        item.getDuracionMinutos()));
            }
            html.append("<br>Clase ").append(escapar(item.getClaseNombre())).append(" · ")
                    .append(item.getCantidad()).append(" × ").append(TextoOrden.tipoPasajero(item.getTipoPasajero()));
            List<String> asientos = item.getAsientos();
            if (asientos != null && !asientos.isEmpty()) {
                html.append("<br>").append(asientos.size() == 1 ? "Asiento: " : "Asientos: ")
                        .append(escapar(String.join(", ", asientos)));
            }
            html.append("</span></td>");
            html.append("<td style=\"padding:12px 0;border-bottom:1px solid ").append(BORDE)
                    .append(";text-align:right;white-space:nowrap;vertical-align:top\">")
                    .append(TextoOrden.plata(item.getSubtotal())).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;margin-top:12px\">");
        if (orden.getMillasUsadas() > 0) {
            html.append("<tr><td style=\"text-align:right;padding:2px 0;color:").append(GRIS).append("\">Millas usadas: ")
                    .append(orden.getMillasUsadas()).append(" (−").append(TextoOrden.plata(orden.getDescuentoMillas()))
                    .append(")</td></tr>");
        }
        html.append("<tr><td style=\"text-align:right;padding:6px 0;font-size:20px;color:").append(AZUL)
                .append("\"><strong>Total: ").append(TextoOrden.plata(orden.getTotal())).append("</strong></td></tr>");
        if (orden.getMillasGanadas() > 0) {
            html.append("<tr><td style=\"text-align:right;padding:2px 0;color:").append(GRIS)
                    .append(";font-size:13px\">Sumaste ").append(orden.getMillasGanadas())
                    .append(" millas con esta compra.</td></tr>");
        }
        html.append("</table>");

        agregarPasesDeEmbarque(html, orden, codigosQr);

        if (conPdf) {
            html.append("<p style=\"font-size:14px;margin:20px 0 0\">Te adjuntamos la <strong>orden de compra en PDF</strong> ")
                    .append("por si la querés guardar o imprimir.</p>");
        }

        LocalDateTime cancelableHasta = orden.getCancelableHasta();
        if (cancelableHasta != null) {
            html.append("<p style=\"color:").append(GRIS).append(";font-size:13px;margin:24px 0 0\">Podés cancelar la compra hasta el ")
                    .append(TextoOrden.FECHA.format(cancelableHasta)).append(" hs desde la sección «Mis compras».</p>");
        }
        html.append("<p style=\"font-size:15px;margin:16px 0 0\"><strong>¡Gracias por volar con nosotros!</strong></p>");
        cerrarMail(html);
        return html.toString();
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarBuenViaje(PasajeroEmbarcadoEvent evento) {
        if (!habilitado) {
            return;
        }
        EmbarqueResponse embarque = evento.embarque();
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "BCA Airlines");
            helper.setTo(evento.destinatario());
            helper.setSubject("BCA Airlines · ¡Buen viaje, " + embarque.getNombre() + "! Embarque validado en el vuelo "
                    + embarque.getNumeroVuelo());
            helper.setText(armarBuenViaje(evento), true);
            helper.addInline("logo", new ClassPathResource("mail/logo.png"), "image/png");
            helper.addInline("banner", new ClassPathResource("mail/banner-buen-viaje.jpg"), "image/jpeg");
            mailSender.send(mensaje);
            log.info("Mail de buen viaje del pase {} enviado", embarque.getCodigoEmbarque());
        } catch (Exception e) {
            // El pasajero ya embarco: que falle el mail no puede deshacer el embarque
            log.warn("No se pudo enviar el mail de buen viaje del pase {}: {}", embarque.getCodigoEmbarque(), e.getMessage());
        }
    }

    private String armarBuenViaje(PasajeroEmbarcadoEvent evento) {
        EmbarqueResponse embarque = evento.embarque();
        String pasajero = embarque.getNombre() + " " + embarque.getApellido();
        StringBuilder html = new StringBuilder();
        abrirMail(html, pasajero + " ya embarcó en el vuelo " + embarque.getNumeroVuelo() + ". ¡Buen viaje!",
                "Embarque confirmado. ¡Buen viaje!");

        html.append("<p style=\"font-size:19px;margin:0 0 10px\"><strong>Hola ").append(escapar(evento.nombreComprador()))
                .append(",</strong></p>");
        html.append("<p style=\"font-size:15px;line-height:22px;margin:0 0 20px\">Te avisamos que se validó el pase de embarque de <strong>")
                .append(escapar(pasajero)).append("</strong>: ya está a bordo del vuelo ")
                .append(escapar(embarque.getNumeroVuelo())).append(".</p>");

        // El pase ya validado, con el mismo diseño que los pases del mail de compra pero en verde
        html.append("<table role=\"presentation\" width=\"100%\" style=\"border:1px solid #b9cbe0;")
                .append("border-radius:10px;border-collapse:separate;border-spacing:0\">");
        html.append("<tr><td bgcolor=\"").append(VERDE).append("\" style=\"background:").append(VERDE)
                .append(";color:#ffffff;padding:9px 16px;font-size:12px;font-weight:bold;letter-spacing:2px;")
                .append("border-radius:9px 0 0 0\">&#10003; EMBARQUE VALIDADO</td>");
        html.append("<td bgcolor=\"").append(VERDE).append("\" style=\"background:").append(VERDE)
                .append(";color:#d7f5e3;padding:9px 16px;font-size:12px;text-align:right;")
                .append("border-radius:0 9px 0 0\">Vuelo ").append(escapar(embarque.getNumeroVuelo()))
                .append("</td></tr>");
        html.append("<tr><td colspan=\"2\" style=\"padding:14px 16px\">");
        html.append(etiqueta("Pasajero")).append("<br>");
        html.append("<strong style=\"font-size:18px\">").append(escapar(pasajero)).append("</strong><br>");
        html.append("<strong style=\"font-size:15px;color:").append(AZUL).append(";line-height:28px\">")
                .append(escapar(embarque.getOrigen())).append(" → ").append(escapar(embarque.getDestino()))
                .append("</strong><br>");
        html.append("<span style=\"color:").append(GRIS).append(";font-size:13px;line-height:20px\">");
        if (embarque.getFechaSalida() != null) {
            html.append(TextoOrden.horario(embarque.getFechaSalida(), embarque.getFechaLlegada(),
                    embarque.getDuracionMinutos())).append("<br>");
        }
        html.append("Clase ").append(escapar(embarque.getClaseNombre())).append(" · ")
                .append(embarque.getAsiento() == null ? "viaja en brazos (sin asiento)" : "Asiento <strong>" + escapar(embarque.getAsiento()) + "</strong>");
        html.append("</span></td></tr>");
        html.append("<tr><td colspan=\"2\" bgcolor=\"#f4f7fa\" style=\"background:#f4f7fa;padding:10px 16px;")
                .append("border-top:2px dashed #b9cbe0;border-radius:0 0 9px 9px\">")
                .append(etiqueta("Validado el")).append("<br><strong style=\"font-size:15px\">");
        if (embarque.getEmbarcadoEn() != null) {
            html.append(TextoOrden.FECHA.format(embarque.getEmbarcadoEn())).append(" hs · ");
        }
        html.append("<span style=\"font-family:'Courier New',Courier,monospace;letter-spacing:2px;color:").append(AZUL)
                .append("\">").append(escapar(embarque.getCodigoEmbarque())).append("</span></strong></td></tr></table>");

        html.append("<p style=\"font-size:15px;line-height:22px;margin:24px 0 0\">Te deseamos un excelente viaje a <strong>")
                .append(escapar(embarque.getDestino())).append("</strong>. ¡Gracias por volar con nosotros!</p>");
        cerrarMail(html);
        return html.toString();
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarAvisoVuelo(VueloModificadoEvent evento) {
        if (!habilitado) {
            return;
        }
        // Un mail por comprador: si falla uno (direccion mala, SMTP caido un momento) se sigue con los demas
        for (VueloModificadoEvent.Comprador comprador : evento.compradores()) {
            try {
                MimeMessage mensaje = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
                helper.setFrom(remitente, "BCA Airlines");
                helper.setTo(comprador.mail());
                helper.setSubject("BCA Airlines · " + asuntoDelAviso(evento));
                helper.setText(armarAvisoVuelo(evento, comprador), true);
                helper.addInline("logo", new ClassPathResource("mail/logo.png"), "image/png");
                helper.addInline("banner", new ClassPathResource("mail/banner-aviso-vuelo.jpg"), "image/jpeg");
                mailSender.send(mensaje);
                log.info("Aviso del vuelo {} ({}) enviado a un comprador", evento.numeroVuelo(), evento.motivo());
            } catch (Exception e) {
                // El cambio del vuelo ya quedo guardado: que falle el mail no lo deshace
                log.warn("No se pudo enviar el aviso del vuelo {}: {}", evento.numeroVuelo(), e.getMessage());
            }
        }
    }

    private String asuntoDelAviso(VueloModificadoEvent evento) {
        String vuelo = "Tu vuelo " + evento.numeroVuelo();
        return switch (evento.motivo()) {
            case DEMORADO -> vuelo + " está demorado";
            case NORMALIZADO -> vuelo + " vuelve a operar con normalidad";
            case CANCELADO -> vuelo + " fue cancelado";
            case REPROGRAMADO -> vuelo + " cambió de horario";
        };
    }

    private String armarAvisoVuelo(VueloModificadoEvent evento, VueloModificadoEvent.Comprador comprador) {
        String vuelo = "<strong>" + escapar(evento.numeroVuelo()) + "</strong> de " + escapar(evento.origen())
                + " a " + escapar(evento.destino());
        // Cada motivo tiene su color y su rotulo en la tarjeta, como en el tablero de partidas
        String color;
        String rotulo;
        String introduccion;
        String cierre;
        switch (evento.motivo()) {
            case DEMORADO -> {
                color = NARANJA;
                rotulo = "VUELO DEMORADO";
                introduccion = "Te avisamos que tu vuelo " + vuelo + " está <strong>demorado</strong>.";
                cierre = "Todavía no hay un nuevo horario confirmado: apenas lo tengamos te vamos a avisar por este medio. "
                        + "Tus pases de embarque siguen valiendo y podés ver el estado del vuelo en «Mis compras».";
            }
            case NORMALIZADO -> {
                color = VERDE;
                rotulo = "VUELO EN HORARIO";
                introduccion = "¡Buenas noticias! Tu vuelo " + vuelo + " vuelve a operar <strong>con normalidad</strong>.";
                cierre = "Tus pases de embarque siguen valiendo: no tenés que hacer nada.";
            }
            case CANCELADO -> {
                color = ROJO;
                rotulo = "VUELO CANCELADO";
                introduccion = "Lamentamos avisarte que tu vuelo " + vuelo + " fue <strong>cancelado</strong>.";
                cierre = "Podés cancelar tu compra desde «Mis compras» cuando quieras, aunque falten menos de "
                        + horasParaCancelar + " horas para la salida: se liberan los asientos y te devolvemos las millas "
                        + "que hayas usado. Disculpá las molestias.";
            }
            default -> {
                color = AZUL;
                rotulo = "NUEVO HORARIO";
                introduccion = "Te avisamos que tu vuelo " + vuelo + " <strong>cambió de horario</strong>.";
                cierre = "Tus pases de embarque siguen valiendo con el nuevo horario. Si no te sirve, podés cancelar la "
                        + "compra desde «Mis compras» hasta " + horasParaCancelar + " horas antes de la salida.";
            }
        }

        StringBuilder html = new StringBuilder();
        abrirMail(html, asuntoDelAviso(evento) + ".", "Aviso de vuelo: novedades de tu vuelo");
        html.append("<p style=\"font-size:19px;margin:0 0 10px\"><strong>Hola ").append(escapar(comprador.nombre()))
                .append(",</strong></p>");
        html.append("<p style=\"font-size:15px;line-height:22px;margin:0 0 20px\">").append(introduccion).append("</p>");

        html.append("<table role=\"presentation\" width=\"100%\" style=\"border:1px solid #b9cbe0;")
                .append("border-radius:10px;border-collapse:separate;border-spacing:0\">");
        html.append("<tr><td bgcolor=\"").append(color).append("\" style=\"background:").append(color)
                .append(";color:#ffffff;padding:9px 16px;font-size:12px;font-weight:bold;letter-spacing:2px;")
                .append("border-radius:9px 0 0 0\">").append(rotulo).append("</td>");
        html.append("<td bgcolor=\"").append(color).append("\" style=\"background:").append(color)
                .append(";color:#ffffff;padding:9px 16px;font-size:12px;text-align:right;")
                .append("border-radius:0 9px 0 0\">Vuelo ").append(escapar(evento.numeroVuelo()))
                .append("</td></tr>");
        html.append("<tr><td colspan=\"2\" style=\"padding:14px 16px;border-radius:0 0 9px 9px\">");
        html.append("<strong style=\"font-size:17px;color:").append(AZUL).append(";line-height:28px\">")
                .append(escapar(evento.origen())).append(" → ").append(escapar(evento.destino()))
                .append("</strong><br>");
        if (evento.motivo() == VueloModificadoEvent.Motivo.REPROGRAMADO && evento.salidaAnterior() != null) {
            html.append(etiqueta("Antes")).append("<br>");
            html.append("<span style=\"color:").append(GRIS).append(";font-size:13px;line-height:20px;text-decoration:line-through\">")
                    .append(horarioDelAviso(evento.salidaAnterior(), evento.llegadaAnterior())).append("</span><br>");
            html.append("<div style=\"height:8px;line-height:8px\">&nbsp;</div>");
            html.append(etiqueta("Ahora")).append("<br>");
            html.append("<strong style=\"font-size:14px;line-height:20px\">")
                    .append(horarioDelAviso(evento.salida(), evento.llegada())).append("</strong>");
        } else {
            html.append("<span style=\"color:").append(GRIS).append(";font-size:13px;line-height:20px\">")
                    .append(horarioDelAviso(evento.salida(), evento.llegada())).append("</span>");
        }
        html.append("</td></tr></table>");

        html.append("<p style=\"font-size:14px;line-height:21px;margin:22px 0 0\">").append(cierre).append("</p>");
        html.append("<p style=\"font-size:15px;margin:16px 0 0\"><strong>Gracias por volar con nosotros.</strong></p>");
        cerrarMail(html);
        return html.toString();
    }

    private String horarioDelAviso(LocalDateTime salida, LocalDateTime llegada) {
        Integer minutos = llegada == null ? null : (int) Duration.between(salida, llegada).toMinutes();
        return TextoOrden.horario(salida, llegada, minutos);
    }

    /**
     * Comienzo comun de los mails: fondo gris, texto de vista previa, cabecera blanca con el logo, el
     * banner (cid:logo y cid:banner, que cada mail adjunta desde resources/mail) y la tarjeta del cuerpo.
     */
    private void abrirMail(StringBuilder html, String vistaPrevia, String textoBanner) {
        html.append("<div style=\"margin:0;padding:24px 12px;background:#eef2f6;").append(FUENTE).append("color:#1c2b3a\">");
        // Texto que algunos clientes muestran como vista previa al lado del asunto
        html.append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0\">")
                .append(escapar(vistaPrevia)).append("</div>");
        html.append("<table role=\"presentation\" align=\"center\" width=\"100%\" ")
                .append("style=\"max-width:600px;margin:0 auto;border-collapse:collapse\">");
        html.append("<tr><td bgcolor=\"#ffffff\" style=\"background:#ffffff;padding:18px 28px;border-radius:10px 10px 0 0\">")
                .append("<img src=\"cid:logo\" width=\"220\" height=\"44\" alt=\"BCA Airlines\" ")
                .append("style=\"display:block;border:0\"></td></tr>");
        html.append("<tr><td style=\"padding:0;line-height:0;font-size:0\">")
                .append("<img src=\"cid:banner\" width=\"600\" alt=\"").append(escapar(textoBanner)).append("\" ")
                .append("style=\"display:block;width:100%;max-width:600px;height:auto;border:0\"></td></tr>");
        html.append("<tr><td bgcolor=\"#ffffff\" style=\"background:#ffffff;padding:28px;border-radius:0 0 10px 10px\">");
    }

    /** Cierra la tarjeta del cuerpo y agrega el pie comun. */
    private void cerrarMail(StringBuilder html) {
        html.append("</td></tr>");
        html.append("<tr><td style=\"padding:16px 8px;text-align:center;font-size:12px;color:").append(GRIS)
                .append("\">Este mail se generó automáticamente, por favor no lo respondas.<br>BCA Airlines</td></tr>");
        html.append("</table></div>");
    }

    /** Franja con los datos de la compra: numero, fecha y medio de pago. */
    private void agregarResumen(StringBuilder html, OrdenResponse orden) {
        html.append("<table role=\"presentation\" width=\"100%\" bgcolor=\"").append(CELESTE)
                .append("\" style=\"background:").append(CELESTE).append(";border-radius:8px;border-collapse:separate;border-spacing:0;margin-bottom:8px\"><tr>");
        html.append(celdaResumen("Compra", "#" + orden.getId()));
        html.append(celdaResumen("Fecha", TextoOrden.FECHA.format(orden.getFecha())));
        if (orden.getMedioPago() != null) {
            html.append(celdaResumen("Medio de pago", TextoOrden.medioPago(orden)));
        }
        html.append("</tr></table>");
    }

    private String celdaResumen(String etiqueta, String valor) {
        return "<td style=\"padding:12px 14px;vertical-align:top\">" + etiqueta(etiqueta)
                + "<br><strong style=\"font-size:14px\">" + escapar(valor) + "</strong></td>";
    }

    /** Una tarjeta por persona y por vuelo, con su asiento, su codigo y el QR que se muestra al embarcar. */
    private void agregarPasesDeEmbarque(StringBuilder html, OrdenResponse orden, Map<String, byte[]> codigosQr) {
        boolean titulo = false;
        for (ItemOrdenResponse item : orden.getItems()) {
            if (item.getPasajes() == null) {
                continue;
            }
            for (PasajeResponse pasaje : item.getPasajes()) {
                if (!titulo) {
                    html.append(titulo("Tus pases de embarque"));
                    html.append("<p style=\"color:").append(GRIS)
                            .append(";font-size:13px;margin:0 0 14px\">Mostrá el QR o el código en el mostrador al embarcar.</p>");
                    titulo = true;
                }
                String cid = "qr-" + pasaje.getCodigoEmbarque();
                codigosQr.put(cid, qrService.png(pasaje.getCodigoEmbarque(), LADO_QR));

                html.append("<table role=\"presentation\" width=\"100%\" style=\"border:1px solid #b9cbe0;")
                        .append("border-radius:10px;border-collapse:separate;border-spacing:0;margin-bottom:16px\">");
                // Cabecera del pase
                html.append("<tr><td bgcolor=\"").append(AZUL).append("\" style=\"background:").append(AZUL)
                        .append(";color:#ffffff;padding:9px 16px;font-size:12px;font-weight:bold;letter-spacing:2px;")
                        .append("border-radius:9px 0 0 0\">PASE DE EMBARQUE</td>");
                html.append("<td bgcolor=\"").append(AZUL).append("\" style=\"background:").append(AZUL)
                        .append(";color:#bcd3e8;padding:9px 16px;font-size:12px;text-align:right;")
                        .append("border-radius:0 9px 0 0\">Vuelo ").append(escapar(item.getNumeroVuelo()))
                        .append("</td></tr>");
                // Datos del pasajero y QR
                html.append("<tr><td style=\"padding:14px 16px;vertical-align:top\">");
                html.append(etiqueta("Pasajero")).append("<br>");
                html.append("<strong style=\"font-size:18px\">").append(escapar(pasaje.getNombre())).append(" ")
                        .append(escapar(pasaje.getApellido())).append("</strong><br>");
                html.append("<strong style=\"font-size:15px;color:").append(AZUL).append(";line-height:28px\">")
                        .append(escapar(item.getOrigen())).append(" → ").append(escapar(item.getDestino()))
                        .append("</strong><br>");
                html.append("<span style=\"color:").append(GRIS).append(";font-size:13px;line-height:20px\">");
                if (item.getFechaSalida() != null) {
                    html.append(TextoOrden.horario(item.getFechaSalida(), item.getFechaLlegada(),
                            item.getDuracionMinutos())).append("<br>");
                }
                html.append("Clase ").append(escapar(item.getClaseNombre())).append(" · ")
                        .append(pasaje.getAsiento() == null ? "viaja en brazos (sin asiento)" : "Asiento <strong>" + escapar(pasaje.getAsiento()) + "</strong>");
                html.append("</span></td>");
                html.append("<td style=\"padding:14px 16px;text-align:right;width:").append(LADO_QR + 10).append("px;vertical-align:middle\">")
                        .append("<img src=\"cid:").append(cid).append("\" width=\"").append(LADO_QR)
                        .append("\" height=\"").append(LADO_QR).append("\" alt=\"QR del pase ")
                        .append(escapar(pasaje.getCodigoEmbarque())).append("\" style=\"display:block;margin-left:auto\">")
                        .append("</td></tr>");
                // Pie con el codigo, que se puede dictar si no se puede escanear
                html.append("<tr><td colspan=\"2\" bgcolor=\"#f4f7fa\" style=\"background:#f4f7fa;padding:10px 16px;")
                        .append("border-top:2px dashed #b9cbe0;border-radius:0 0 9px 9px\">")
                        .append(etiqueta("Código de embarque")).append("<br>")
                        .append("<strong style=\"font-family:'Courier New',Courier,monospace;font-size:20px;letter-spacing:3px;color:")
                        .append(AZUL).append("\">").append(escapar(pasaje.getCodigoEmbarque())).append("</strong>")
                        .append("</td></tr></table>");
            }
        }
    }

    private String titulo(String texto) {
        return "<h3 style=\"color:" + AZUL + ";font-size:16px;margin:26px 0 10px;padding-bottom:6px;border-bottom:2px solid "
                + AZUL + "\">" + escapar(texto) + "</h3>";
    }

    private String etiqueta(String texto) {
        return "<span style=\"font-size:11px;letter-spacing:1px;text-transform:uppercase;color:" + GRIS + "\">"
                + escapar(texto) + "</span>";
    }

    private String escapar(String texto) {
        return HtmlUtils.htmlEscape(texto == null ? "" : texto);
    }
}
