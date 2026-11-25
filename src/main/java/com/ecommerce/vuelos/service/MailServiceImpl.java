package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.ItemOrdenResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.entity.MedioPago;
import com.ecommerce.vuelos.entity.TipoPasajero;
import com.ecommerce.vuelos.event.OrdenConfirmadaEvent;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);
    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private JavaMailSender mailSender;

    // Con mail.habilitado=false (por ejemplo en una demo sin SMTP) no se intenta mandar nada
    @Value("${mail.habilitado}")
    private boolean habilitado;
    @Value("${mail.remitente}")
    private String remitente;

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
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");
            helper.setFrom(remitente, "BCA Airlines");
            helper.setTo(evento.destinatario());
            helper.setSubject("BCA Airlines · Confirmación de tu compra #" + orden.getId());
            helper.setText(armarCuerpo(evento), true);
            mailSender.send(mensaje);
            log.info("Mail de confirmacion de la compra #{} enviado", orden.getId());
        } catch (Exception e) {
            // El pago ya esta confirmado: que falle el mail no puede deshacer la compra
            log.warn("No se pudo enviar el mail de la compra #{}: {}", orden.getId(), e.getMessage());
        }
    }

    private String armarCuerpo(OrdenConfirmadaEvent evento) {
        OrdenResponse orden = evento.orden();
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1c2b3a\">");
        html.append("<h2 style=\"color:#0b3d6b;margin-bottom:4px\">BCA Airlines</h2>");
        html.append("<p>Hola ").append(escapar(evento.nombre())).append(", ¡tu compra está confirmada!</p>");
        html.append("<p style=\"margin:0\"><strong>Número de compra:</strong> #").append(orden.getId()).append("</p>");
        html.append("<p style=\"margin:0\"><strong>Fecha:</strong> ").append(FECHA.format(orden.getFecha())).append("</p>");
        if (orden.getMedioPago() != null) {
            html.append("<p style=\"margin:0\"><strong>Medio de pago:</strong> ")
                    .append(escapar(textoMedioPago(orden))).append("</p>");
        }

        html.append("<table style=\"width:100%;border-collapse:collapse;margin-top:16px\">");
        for (ItemOrdenResponse item : orden.getItems()) {
            html.append("<tr><td style=\"padding:10px 0;border-bottom:1px solid #dde3ea\">");
            html.append("<strong>").append(escapar(item.getOrigen())).append(" → ")
                    .append(escapar(item.getDestino())).append("</strong><br>");
            html.append("<span style=\"color:#5b6b7b;font-size:13px\">Vuelo ").append(escapar(item.getNumeroVuelo()));
            if (item.getFechaSalida() != null) {
                html.append(" · sale el ").append(FECHA.format(item.getFechaSalida())).append(" hs");
            }
            html.append("<br>Clase ").append(escapar(item.getClaseNombre())).append(" · ")
                    .append(item.getCantidad()).append(" × ").append(nombreTipo(item.getTipoPasajero()));
            List<String> asientos = item.getAsientos();
            if (asientos != null && !asientos.isEmpty()) {
                html.append("<br>").append(asientos.size() == 1 ? "Asiento: " : "Asientos: ")
                        .append(escapar(String.join(", ", asientos)));
            }
            html.append("</span></td>");
            html.append("<td style=\"padding:10px 0;border-bottom:1px solid #dde3ea;text-align:right;white-space:nowrap\">")
                    .append(plata(item.getSubtotal())).append("</td></tr>");
        }
        html.append("</table>");

        if (orden.getMillasUsadas() > 0) {
            html.append("<p style=\"text-align:right;margin:8px 0 0\">Millas usadas: ").append(orden.getMillasUsadas())
                    .append(" (−").append(plata(orden.getDescuentoMillas())).append(")</p>");
        }
        html.append("<p style=\"text-align:right;font-size:18px;margin:8px 0 0\"><strong>Total: ")
                .append(plata(orden.getTotal())).append("</strong></p>");
        if (orden.getMillasGanadas() > 0) {
            html.append("<p style=\"text-align:right;color:#5b6b7b;margin:4px 0 0\">Sumaste ")
                    .append(orden.getMillasGanadas()).append(" millas con esta compra.</p>");
        }

        LocalDateTime cancelableHasta = orden.getCancelableHasta();
        if (cancelableHasta != null) {
            html.append("<p style=\"color:#5b6b7b;font-size:13px;margin-top:20px\">Podés cancelar la compra hasta el ")
                    .append(FECHA.format(cancelableHasta)).append(" hs desde la sección «Mis compras».</p>");
        }
        html.append("<p style=\"color:#5b6b7b;font-size:13px\">¡Gracias por volar con nosotros!</p>");
        html.append("</div>");
        return html.toString();
    }

    private String plata(BigDecimal monto) {
        if (monto == null || monto.signum() == 0) {
            return "Gratis";
        }
        return NumberFormat.getCurrencyInstance(ARGENTINA).format(monto);
    }

    private String nombreTipo(TipoPasajero tipo) {
        return switch (tipo) {
            case ADULTO -> "adulto";
            case NINO -> "niño";
            case BEBE -> "bebé";
        };
    }

    private String textoMedioPago(OrdenResponse orden) {
        MedioPago medio = orden.getMedioPago();
        String nombre = switch (medio) {
            case TARJETA_CREDITO -> "Tarjeta de crédito";
            case TARJETA_DEBITO -> "Tarjeta de débito";
            case TRANSFERENCIA -> "Transferencia";
            case BILLETERA_VIRTUAL -> "Billetera virtual";
            case MILLAS -> "Millas";
        };
        return orden.getUltimosDigitos() == null ? nombre : nombre + " terminada en " + orden.getUltimosDigitos();
    }

    private String escapar(String texto) {
        return HtmlUtils.htmlEscape(texto == null ? "" : texto);
    }
}
