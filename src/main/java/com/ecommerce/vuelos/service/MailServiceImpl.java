package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.ItemOrdenResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.dto.orden.PasajeResponse;
import com.ecommerce.vuelos.entity.MedioPago;
import com.ecommerce.vuelos.entity.TipoPasajero;
import com.ecommerce.vuelos.event.OrdenConfirmadaEvent;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);
    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int LADO_QR = 120;

    // Los mails no cargan hojas de estilo: todo el diseño va en estilos en linea y con tablas
    private static final String AZUL = "#0b3d6b";
    private static final String CELESTE = "#e8f1f9";
    private static final String GRIS = "#5b6b7b";
    private static final String BORDE = "#dde3ea";
    private static final String FUENTE = "font-family:Arial,Helvetica,sans-serif;";

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private QrService qrService;

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
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "BCA Airlines");
            helper.setTo(evento.destinatario());
            helper.setSubject("BCA Airlines · Confirmación de tu compra #" + orden.getId());
            // Los QR viajan como imagenes adjuntas dentro del mail (cid:): los clientes de mail no muestran SVG
            Map<String, byte[]> codigosQr = new LinkedHashMap<>();
            helper.setText(armarCuerpo(evento, codigosQr), true);
            for (Map.Entry<String, byte[]> qr : codigosQr.entrySet()) {
                helper.addInline(qr.getKey(), new ByteArrayResource(qr.getValue()), "image/png");
            }
            mailSender.send(mensaje);
            log.info("Mail de confirmacion de la compra #{} enviado", orden.getId());
        } catch (Exception e) {
            // El pago ya esta confirmado: que falle el mail no puede deshacer la compra
            log.warn("No se pudo enviar el mail de la compra #{}: {}", orden.getId(), e.getMessage());
        }
    }

    private String armarCuerpo(OrdenConfirmadaEvent evento, Map<String, byte[]> codigosQr) {
        OrdenResponse orden = evento.orden();
        StringBuilder html = new StringBuilder();
        html.append("<div style=\"margin:0;padding:24px 12px;background:#eef2f6;").append(FUENTE).append("color:#1c2b3a\">");
        // Texto que algunos clientes muestran como vista previa al lado del asunto
        html.append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0\">Tu compra #")
                .append(orden.getId()).append(" está confirmada. Acá tenés tus pases de embarque.</div>");
        html.append("<table role=\"presentation\" align=\"center\" width=\"100%\" ")
                .append("style=\"max-width:600px;margin:0 auto;border-collapse:collapse\">");

        html.append("<tr><td bgcolor=\"").append(AZUL).append("\" style=\"background:").append(AZUL)
                .append(";padding:22px 28px;border-radius:10px 10px 0 0\">")
                .append("<span style=\"font-size:22px;font-weight:bold;letter-spacing:3px;color:#ffffff\">BCA AIRLINES</span><br>")
                .append("<span style=\"font-size:13px;color:#bcd3e8\">Confirmación de compra</span></td></tr>");

        html.append("<tr><td bgcolor=\"#ffffff\" style=\"background:#ffffff;padding:28px;border-radius:0 0 10px 10px\">");
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
            html.append("<td style=\"padding:12px 0;border-bottom:1px solid ").append(BORDE)
                    .append(";text-align:right;white-space:nowrap;vertical-align:top\">")
                    .append(plata(item.getSubtotal())).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;margin-top:12px\">");
        if (orden.getMillasUsadas() > 0) {
            html.append("<tr><td style=\"text-align:right;padding:2px 0;color:").append(GRIS).append("\">Millas usadas: ")
                    .append(orden.getMillasUsadas()).append(" (−").append(plata(orden.getDescuentoMillas()))
                    .append(")</td></tr>");
        }
        html.append("<tr><td style=\"text-align:right;padding:6px 0;font-size:20px;color:").append(AZUL)
                .append("\"><strong>Total: ").append(plata(orden.getTotal())).append("</strong></td></tr>");
        if (orden.getMillasGanadas() > 0) {
            html.append("<tr><td style=\"text-align:right;padding:2px 0;color:").append(GRIS)
                    .append(";font-size:13px\">Sumaste ").append(orden.getMillasGanadas())
                    .append(" millas con esta compra.</td></tr>");
        }
        html.append("</table>");

        agregarPasesDeEmbarque(html, orden, codigosQr);

        LocalDateTime cancelableHasta = orden.getCancelableHasta();
        if (cancelableHasta != null) {
            html.append("<p style=\"color:").append(GRIS).append(";font-size:13px;margin:24px 0 0\">Podés cancelar la compra hasta el ")
                    .append(FECHA.format(cancelableHasta)).append(" hs desde la sección «Mis compras».</p>");
        }
        html.append("<p style=\"font-size:15px;margin:16px 0 0\"><strong>¡Gracias por volar con nosotros!</strong></p>");
        html.append("</td></tr>");

        html.append("<tr><td style=\"padding:16px 8px;text-align:center;font-size:12px;color:").append(GRIS)
                .append("\">Este mail se generó automáticamente, por favor no lo respondas.<br>BCA Airlines</td></tr>");
        html.append("</table></div>");
        return html.toString();
    }

    /** Franja con los datos de la compra: numero, fecha y medio de pago. */
    private void agregarResumen(StringBuilder html, OrdenResponse orden) {
        html.append("<table role=\"presentation\" width=\"100%\" bgcolor=\"").append(CELESTE)
                .append("\" style=\"background:").append(CELESTE).append(";border-radius:8px;border-collapse:separate;border-spacing:0;margin-bottom:8px\"><tr>");
        html.append(celdaResumen("Compra", "#" + orden.getId()));
        html.append(celdaResumen("Fecha", FECHA.format(orden.getFecha())));
        if (orden.getMedioPago() != null) {
            html.append(celdaResumen("Medio de pago", textoMedioPago(orden)));
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
                    html.append("Sale el ").append(FECHA.format(item.getFechaSalida())).append(" hs<br>");
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
