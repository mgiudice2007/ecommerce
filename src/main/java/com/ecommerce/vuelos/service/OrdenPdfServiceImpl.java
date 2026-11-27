package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.ItemOrdenResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.dto.orden.PasajeResponse;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.ecommerce.vuelos.entity.TipoPasajero;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Itinerario y recibo de la compra, como el que manda una aerolinea: un resumen arriba, una tarjeta por
 * vuelo con sus pasajeros, el detalle del pago y la informacion importante. Cada bloque se mantiene
 * entero en una pagina y todas las paginas llevan el pie con el numero de orden.
 */
@Service
public class OrdenPdfServiceImpl implements OrdenPdfService {

    // Mismos colores que el front y el mail
    private static final Color AZUL = new Color(0x00, 0x57, 0xB8);
    private static final Color AZUL_NOCHE = new Color(0x04, 0x1E, 0x42);
    private static final Color NARANJA = new Color(0xF2, 0x60, 0x0C);
    private static final Color GRIS = new Color(0x5B, 0x6B, 0x7B);
    private static final Color CELESTE = new Color(0xE8, 0xF1, 0xF9);
    private static final Color FONDO = new Color(0xF5, 0xF7, 0xFA);
    private static final Color BORDE = new Color(0xDD, 0xE3, 0xEA);
    private static final Color ROJO = new Color(0xB4, 0x23, 0x18);
    private static final Color ROJO_FONDO = new Color(0xFE, 0xF3, 0xF2);
    // Sobre el azul noche del resumen
    private static final Color ROTULO_CLARO = new Color(0xA9, 0xBF, 0xDB);
    private static final Color VERDE_CLARO = new Color(0x7D, 0xDB, 0xA6);
    private static final Color ROJO_CLARO = new Color(0xFF, 0xA3, 0x97);

    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // A mano para no depender de las abreviaturas del JDK ("ene." con punto, segun la version)
    private static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final String[] DIAS_CORTOS = {"lun", "mar", "mié", "jue", "vie", "sáb", "dom"};
    private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private static final String[] MESES_CORTOS = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago",
            "sep", "oct", "nov", "dic"};

    // Separacion entre un bloque y el siguiente, y aire dentro de las tarjetas
    private static final float ENTRE_BLOQUES = 16;
    private static final float AIRE = 10;

    @Override
    public byte[] ordenDeCompra(OrdenResponse orden, String nombreCliente, String emailCliente) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 34, 58);
        PdfWriter writer = PdfWriter.getInstance(documento, salida);
        writer.setPageEvent(new PieDePagina(orden.getId()));
        documento.addTitle("Orden de compra N° " + orden.getId());
        documento.addAuthor("BCA Airlines");
        documento.open();

        documento.add(encabezado());
        documento.add(resumen(orden));
        PdfPTable aviso = aviso(orden);
        if (aviso != null) {
            documento.add(aviso);
        }
        documento.add(bloque("Titular de la compra", titular(orden, nombreCliente, emailCliente)));
        agregarItinerario(documento, orden);
        documento.add(bloque("Detalle del pago", pago(orden)));
        documento.add(bloque("Información importante", informacion(orden)));

        documento.close();
        return salida.toByteArray();
    }

    // ---------- Encabezado y resumen ----------

    /** Logo a la izquierda, el nombre del documento a la derecha y una linea naranja abajo. */
    private PdfPTable encabezado() {
        PdfPTable tabla = tabla(new float[]{1, 1});
        PdfPCell logo = sinBorde(new PdfPCell(logo(), false));
        logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logo.setPaddingBottom(10);
        tabla.addCell(logo);

        PdfPCell titulo = celda(Element.ALIGN_RIGHT,
                linea(Element.ALIGN_RIGHT, texto("ITINERARIO Y RECIBO", fuente(13, Font.BOLD, AZUL_NOCHE))),
                linea(Element.ALIGN_RIGHT, texto("Pasaje electrónico", fuente(8.5f, Font.NORMAL, GRIS))));
        titulo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titulo.setPaddingBottom(10);
        tabla.addCell(titulo);

        PdfPCell raya = sinBorde(new PdfPCell());
        raya.setColspan(2);
        raya.setBorder(Rectangle.TOP);
        raya.setBorderColorTop(NARANJA);
        raya.setBorderWidthTop(2.5f);
        raya.setFixedHeight(12);
        tabla.addCell(raya);
        return tabla;
    }

    /** Franja azul con lo que se busca primero: numero de orden, fecha, estado y total. */
    private PdfPTable resumen(OrdenResponse orden) {
        boolean cancelada = orden.getEstado() == EstadoOrden.CANCELADA;
        PdfPTable tabla = tabla(new float[]{0.75f, 1.4f, 0.95f, 1.4f});
        tabla.addCell(datoResumen("N° de orden", String.valueOf(orden.getId()), Color.WHITE, Element.ALIGN_LEFT));
        tabla.addCell(datoResumen("Fecha de compra", orden.getFecha() == null ? "-" : DIA.format(orden.getFecha())
                + " " + HORA.format(orden.getFecha()) + " hs", Color.WHITE, Element.ALIGN_LEFT));
        tabla.addCell(datoResumen("Estado", cancelada ? "Cancelada" : "Confirmada",
                cancelada ? ROJO_CLARO : VERDE_CLARO, Element.ALIGN_LEFT));
        tabla.addCell(datoResumen(cancelada ? "Total" : "Total pagado", TextoOrden.plata(orden.getTotal()),
                Color.WHITE, Element.ALIGN_RIGHT));
        return tabla;
    }

    private PdfPCell datoResumen(String rotulo, String valor, Color colorValor, int alineacion) {
        PdfPCell celda = celda(alineacion,
                linea(alineacion, rotulo(rotulo, ROTULO_CLARO)),
                espaciada(linea(alineacion, texto(valor, fuente(11.5f, Font.BOLD, colorValor))), 3));
        celda.setBackgroundColor(AZUL_NOCHE);
        celda.setPaddingTop(AIRE);
        celda.setPaddingBottom(AIRE + 2);
        celda.setPaddingLeft(12);
        celda.setPaddingRight(12);
        return celda;
    }

    /** Recuadro rojo si la compra se cancelo o si la aerolinea cancelo uno de los vuelos. */
    private PdfPTable aviso(OrdenResponse orden) {
        String titulo;
        String detalle;
        if (orden.getEstado() == EstadoOrden.CANCELADA) {
            titulo = "Compra cancelada";
            detalle = "Los pasajes de esta orden ya no son válidos para viajar.";
        } else if (orden.isVueloCancelado()) {
            titulo = "La aerolínea canceló uno de los vuelos";
            detalle = "Podés cancelar la compra cuando quieras desde «Mis compras», sin el límite de horas habitual.";
        } else {
            return null;
        }
        PdfPTable tabla = tabla(new float[]{1});
        tabla.setSpacingBefore(12);
        PdfPCell celda = celda(Element.ALIGN_LEFT,
                linea(Element.ALIGN_LEFT, texto(titulo, fuente(10, Font.BOLD, ROJO))),
                espaciada(linea(Element.ALIGN_LEFT, texto(detalle, fuente(8.5f, Font.NORMAL, AZUL_NOCHE))), 2));
        celda.setBackgroundColor(ROJO_FONDO);
        celda.setBorder(Rectangle.LEFT);
        celda.setBorderColorLeft(ROJO);
        celda.setBorderWidthLeft(3);
        celda.setPadding(AIRE);
        tabla.addCell(celda);
        return tabla;
    }

    private PdfPTable titular(OrdenResponse orden, String nombreCliente, String emailCliente) {
        PdfPTable tabla = tabla(new float[]{1.1f, 1.5f, 1.6f});
        tabla.addCell(dato("Nombre", nombreCliente));
        tabla.addCell(dato("Email", emailCliente));
        tabla.addCell(dato("Medio de pago", TextoOrden.medioPago(orden)));
        return tabla;
    }

    // ---------- Itinerario ----------

    /** Un vuelo de la compra con todos sus items (adultos, niños y bebes del mismo vuelo van juntos). */
    private record Tramo(ItemOrdenResponse vuelo, List<ItemOrdenResponse> items) {
    }

    private List<Tramo> tramos(OrdenResponse orden) {
        Map<String, List<ItemOrdenResponse>> porVuelo = new LinkedHashMap<>();
        for (ItemOrdenResponse item : orden.getItems()) {
            String clave = item.getVueloId() != null
                    ? String.valueOf(item.getVueloId())
                    : item.getNumeroVuelo() + "|" + item.getFechaSalida();
            porVuelo.computeIfAbsent(clave, k -> new ArrayList<>()).add(item);
        }
        return porVuelo.values().stream().map(items -> new Tramo(items.get(0), items)).toList();
    }

    /** Una tarjeta por vuelo; el titulo de la seccion viaja pegado a la primera para que no quede solo. */
    private void agregarItinerario(Document documento, OrdenResponse orden) {
        List<Tramo> tramos = tramos(orden);
        boolean idaYVuelta = tramos.size() == 2
                && Objects.equals(tramos.get(0).vuelo().getOrigen(), tramos.get(1).vuelo().getDestino())
                && Objects.equals(tramos.get(0).vuelo().getDestino(), tramos.get(1).vuelo().getOrigen());
        for (int i = 0; i < tramos.size(); i++) {
            String rotulo;
            if (tramos.size() == 1) {
                rotulo = "SOLO IDA";
            } else if (idaYVuelta) {
                rotulo = i == 0 ? "IDA" : "VUELTA";
            } else {
                rotulo = "TRAMO " + (i + 1);
            }
            PdfPTable tarjeta = tarjetaVuelo(tramos.get(i), rotulo);
            if (i == 0) {
                documento.add(bloque("Itinerario", tarjeta));
            } else {
                PdfPTable siguiente = bloque(null, tarjeta);
                siguiente.setSpacingBefore(AIRE);
                documento.add(siguiente);
            }
        }
    }

    private PdfPTable tarjetaVuelo(Tramo tramo, String rotulo) {
        ItemOrdenResponse vuelo = tramo.vuelo();
        PdfPTable tarjeta = tabla(new float[]{1});

        // Cabecera celeste: ida/vuelta, numero de vuelo y dia
        PdfPTable cabecera = tabla(new float[]{1, 1});
        Chunk etiqueta = texto(" " + rotulo + " ", fuente(7.5f, Font.BOLD, Color.WHITE));
        etiqueta.setBackground(NARANJA, 2, 2, 2, 2);
        cabecera.addCell(celda(Element.ALIGN_LEFT, linea(Element.ALIGN_LEFT, etiqueta,
                texto("     Vuelo " + vuelo.getNumeroVuelo(), fuente(10, Font.BOLD, AZUL_NOCHE)))));
        cabecera.addCell(celda(Element.ALIGN_RIGHT, linea(Element.ALIGN_RIGHT, texto(
                vuelo.getFechaSalida() == null ? "Fecha a confirmar" : fechaLarga(vuelo.getFechaSalida().toLocalDate()),
                fuente(9, Font.BOLD, AZUL_NOCHE)))));
        PdfPCell filaCabecera = contenedor(cabecera);
        filaCabecera.setBackgroundColor(CELESTE);
        filaCabecera.setPadding(8);
        filaCabecera.setPaddingLeft(12);
        filaCabecera.setPaddingRight(12);
        filaCabecera.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.RIGHT);
        filaCabecera.setBorderColor(BORDE);
        tarjeta.addCell(filaCabecera);

        // Salida, duracion, llegada y datos del vuelo
        PdfPTable recorrido = tabla(new float[]{2.25f, 1.45f, 2.25f, 1.75f});
        recorrido.addCell(extremo(vuelo.getFechaSalida(), vuelo.getOrigenIata(), vuelo.getOrigen(),
                vuelo.getOrigenAeropuerto(), null, Element.ALIGN_LEFT));
        recorrido.addCell(duracion(vuelo));
        recorrido.addCell(extremo(vuelo.getFechaLlegada(), vuelo.getDestinoIata(), vuelo.getDestino(),
                vuelo.getDestinoAeropuerto(), vuelo.getFechaSalida(), Element.ALIGN_RIGHT));
        PdfPCell datos = celda(Element.ALIGN_LEFT,
                linea(Element.ALIGN_LEFT, rotulo("Clase", GRIS)),
                espaciada(linea(Element.ALIGN_LEFT, texto(vuelo.getClaseNombre(), fuente(9.5f, Font.BOLD, AZUL_NOCHE))), 2),
                espaciada(linea(Element.ALIGN_LEFT, rotulo("Pasajeros", GRIS)), 8),
                espaciada(linea(Element.ALIGN_LEFT, texto(cantidadPorTipo(tramo.items()), fuente(9.5f, Font.BOLD, AZUL_NOCHE))), 2));
        datos.setBorder(Rectangle.LEFT);
        datos.setBorderColor(BORDE);
        datos.setPaddingLeft(14);
        datos.setVerticalAlignment(Element.ALIGN_MIDDLE);
        recorrido.addCell(datos);
        PdfPCell filaRecorrido = contenedor(recorrido);
        filaRecorrido.setPadding(14);
        filaRecorrido.setBorder(Rectangle.LEFT | Rectangle.RIGHT);
        filaRecorrido.setBorderColor(BORDE);
        tarjeta.addCell(filaRecorrido);

        // Pasajeros de este vuelo (las compras viejas pueden no tenerlos cargados)
        PdfPTable pasajeros = pasajeros(tramo);
        PdfPCell filaPasajeros = pasajeros == null ? sinBorde(new PdfPCell()) : contenedor(pasajeros);
        filaPasajeros.setPadding(0);
        filaPasajeros.setBorder(Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
        filaPasajeros.setBorderColor(BORDE);
        if (pasajeros == null) {
            filaPasajeros.setFixedHeight(1);
        }
        tarjeta.addCell(filaPasajeros);
        return tarjeta;
    }

    /** Hora grande, codigo y ciudad, aeropuerto y dia. Si llega otro dia, la hora lleva "+1". */
    private PdfPCell extremo(LocalDateTime momento, String iata, String ciudad, String aeropuerto,
                             LocalDateTime salida, int alineacion) {
        List<Paragraph> lineas = new ArrayList<>();
        if (momento == null) {
            lineas.add(linea(alineacion, texto("--:--", fuente(20, Font.BOLD, BORDE))));
        } else {
            Paragraph hora = linea(alineacion, texto(HORA.format(momento), fuente(20, Font.BOLD, AZUL_NOCHE)));
            long dias = salida == null ? 0 : salida.toLocalDate().until(momento.toLocalDate()).getDays();
            if (dias > 0) {
                hora.add(texto(" +" + dias, fuente(9, Font.BOLD, NARANJA)));
            }
            lineas.add(hora);
        }
        Paragraph lugar = linea(alineacion);
        if (iata != null) {
            lugar.add(texto(iata + "  ", fuente(10.5f, Font.BOLD, AZUL)));
        }
        lugar.add(texto(ciudad, fuente(9.5f, Font.BOLD, AZUL_NOCHE)));
        lineas.add(espaciada(lugar, 4));
        if (aeropuerto != null) {
            lineas.add(espaciada(linea(alineacion, texto("Aeropuerto " + aeropuerto, fuente(7.5f, Font.NORMAL, GRIS))), 2));
        }
        lineas.add(espaciada(linea(alineacion, texto(momento == null ? "Horario a confirmar"
                : fechaCorta(momento.toLocalDate()), fuente(7.5f, Font.NORMAL, GRIS))), 1));
        PdfPCell celda = celda(alineacion, lineas.toArray(new Paragraph[0]));
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    /** Duracion arriba, una linea con el avion en el medio y "Directo" abajo. */
    private PdfPCell duracion(ItemOrdenResponse vuelo) {
        PdfPTable medio = tabla(new float[]{1, 0.5f, 1});
        PdfPCell arriba = celda(Element.ALIGN_CENTER, linea(Element.ALIGN_CENTER,
                texto(vuelo.getDuracionMinutos() == null ? " " : TextoOrden.duracion(vuelo.getDuracionMinutos()),
                        fuente(8.5f, Font.BOLD, GRIS))));
        arriba.setColspan(3);
        arriba.setPaddingBottom(3);
        medio.addCell(arriba);
        medio.addCell(trazo());
        // En ZapfDingbats el caracter "(" es el avion
        PdfPCell avion = celda(Element.ALIGN_CENTER, linea(Element.ALIGN_CENTER,
                texto("(", new Font(Font.ZAPFDINGBATS, 11, Font.NORMAL, AZUL))));
        avion.setVerticalAlignment(Element.ALIGN_MIDDLE);
        medio.addCell(avion);
        medio.addCell(trazo());
        PdfPCell abajo = celda(Element.ALIGN_CENTER, linea(Element.ALIGN_CENTER,
                texto("Directo", fuente(7.5f, Font.NORMAL, GRIS))));
        abajo.setColspan(3);
        abajo.setPaddingTop(3);
        medio.addCell(abajo);

        PdfPCell celda = contenedor(medio);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPaddingLeft(6);
        celda.setPaddingRight(6);
        return celda;
    }

    private PdfPCell trazo() {
        PdfPCell celda = sinBorde(new PdfPCell());
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColorBottom(BORDE);
        celda.setBorderWidthBottom(1.2f);
        celda.setFixedHeight(7);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    private PdfPTable pasajeros(Tramo tramo) {
        PdfPTable tabla = tabla(new float[]{3.0f, 0.8f, 1.45f, 0.85f, 1.6f});
        boolean hay = false;
        for (String titulo : new String[]{"Pasajero", "Tipo", "Documento", "Asiento", "Código de embarque"}) {
            PdfPCell celda = celda(Element.ALIGN_LEFT, linea(Element.ALIGN_LEFT, rotulo(titulo, GRIS)));
            celda.setBackgroundColor(FONDO);
            celda.setBorder(Rectangle.TOP);
            celda.setBorderColor(BORDE);
            celda.setPaddingTop(6);
            celda.setPaddingBottom(6);
            relleno(celda, titulo.equals("Pasajero"), titulo.startsWith("Código"));
            tabla.addCell(celda);
        }
        for (ItemOrdenResponse item : tramo.items()) {
            if (item.getPasajes() == null) {
                continue;
            }
            for (PasajeResponse pasaje : item.getPasajes()) {
                hay = true;
                String documento = pasaje.getTipoDocumento() == null
                        ? pasaje.getDocumento()
                        : pasaje.getTipoDocumento() + " " + pasaje.getDocumento();
                tabla.addCell(filaPasajero(texto(nombrePasajero(pasaje), fuente(9, Font.BOLD, AZUL_NOCHE)), true, false));
                tabla.addCell(filaPasajero(texto(mayuscula(TextoOrden.tipoPasajero(item.getTipoPasajero())),
                        fuente(9, Font.NORMAL, AZUL_NOCHE)), false, false));
                tabla.addCell(filaPasajero(texto(documento, fuente(9, Font.NORMAL, AZUL_NOCHE)), false, false));
                tabla.addCell(filaPasajero(pasaje.getAsiento() == null
                        ? texto("En brazos", fuente(9, Font.ITALIC, GRIS))
                        : texto(pasaje.getAsiento(), fuente(9, Font.BOLD, AZUL_NOCHE)), false, false));
                tabla.addCell(filaPasajero(texto(pasaje.getCodigoEmbarque(), new Font(Font.COURIER, 9.5f, Font.BOLD, AZUL)),
                        false, true));
            }
        }
        return hay ? tabla : null;
    }

    private PdfPCell filaPasajero(Chunk contenido, boolean primera, boolean ultima) {
        PdfPCell celda = celda(Element.ALIGN_LEFT, linea(Element.ALIGN_LEFT, contenido));
        celda.setBorder(Rectangle.TOP);
        celda.setBorderColor(BORDE);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPaddingTop(6);
        celda.setPaddingBottom(7);
        relleno(celda, primera, ultima);
        return celda;
    }

    /** La primera y la ultima columna se separan del borde de la tarjeta igual que el resto del contenido. */
    private void relleno(PdfPCell celda, boolean primera, boolean ultima) {
        celda.setPaddingLeft(primera ? 14 : 5);
        celda.setPaddingRight(ultima ? 14 : 5);
    }

    /** "GÓMEZ, Ana": el apellido primero y en mayusculas, como en un pasaje. */
    private String nombrePasajero(PasajeResponse pasaje) {
        return pasaje.getApellido().toUpperCase(ARGENTINA) + ", " + pasaje.getNombre();
    }

    /** "2 adultos · 1 bebé" */
    private String cantidadPorTipo(List<ItemOrdenResponse> items) {
        Map<TipoPasajero, Integer> cantidades = new EnumMap<>(TipoPasajero.class);
        for (ItemOrdenResponse item : items) {
            cantidades.merge(item.getTipoPasajero(), item.getCantidad(), Integer::sum);
        }
        List<String> partes = new ArrayList<>();
        cantidades.forEach((tipo, cantidad) ->
                partes.add(cantidad + " " + TextoOrden.tipoPasajero(tipo) + (cantidad == 1 ? "" : "s")));
        return String.join(" · ", partes);
    }

    // ---------- Pago ----------

    private PdfPTable pago(OrdenResponse orden) {
        PdfPTable tabla = tabla(new float[]{4.4f, 0.8f, 1.7f, 1.7f});
        String[] titulos = {"Descripción", "Cant.", "Precio unit.", "Importe"};
        for (int i = 0; i < titulos.length; i++) {
            int alineacion = i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT;
            PdfPCell celda = celda(alineacion, linea(alineacion, rotulo(titulos[i], GRIS)));
            celda.setBackgroundColor(FONDO);
            celda.setPadding(6);
            celda.setPaddingLeft(i == 0 ? 10 : 6);
            celda.setPaddingRight(i == titulos.length - 1 ? 10 : 6);
            tabla.addCell(celda);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemOrdenResponse item : orden.getItems()) {
            subtotal = subtotal.add(item.getSubtotal());
            PdfPCell descripcion = celda(Element.ALIGN_LEFT,
                    linea(Element.ALIGN_LEFT, texto("Vuelo " + item.getNumeroVuelo() + "  ·  " + item.getOrigen()
                            + " - " + item.getDestino(), fuente(9, Font.BOLD, AZUL_NOCHE))),
                    espaciada(linea(Element.ALIGN_LEFT, texto("Clase " + item.getClaseNombre() + "  ·  Tarifa "
                            + TextoOrden.tipoPasajero(item.getTipoPasajero()), fuente(8, Font.NORMAL, GRIS))), 1));
            tabla.addCell(filaPago(descripcion, true, false));
            tabla.addCell(filaPago(celdaNumero(String.valueOf(item.getCantidad()), false), false, false));
            tabla.addCell(filaPago(celdaNumero(TextoOrden.plata(item.getPrecioUnitario()), false), false, false));
            tabla.addCell(filaPago(celdaNumero(TextoOrden.plata(item.getSubtotal()), true), false, true));
        }

        filaTotal(tabla, "Subtotal", TextoOrden.plata(subtotal), false, true);
        if (orden.getMillasUsadas() != null && orden.getMillasUsadas() > 0) {
            filaTotal(tabla, "Pagado con " + entero(orden.getMillasUsadas()) + " millas",
                    "- " + TextoOrden.plata(orden.getDescuentoMillas()), false, false);
        }
        boolean cancelada = orden.getEstado() == EstadoOrden.CANCELADA;
        filaTotal(tabla, cancelada ? "Total" : "Total pagado", TextoOrden.plata(orden.getTotal()), true, false);

        // Notas: con que se pago, las millas sumadas y el ahorro por promociones
        List<Paragraph> notas = new ArrayList<>();
        notas.add(linea(Element.ALIGN_LEFT, texto("Medio de pago: ", fuente(8, Font.BOLD, GRIS)),
                texto(TextoOrden.medioPago(orden), fuente(8, Font.NORMAL, GRIS))));
        if (orden.getDescuentoTotal() != null && orden.getDescuentoTotal().signum() > 0) {
            notas.add(espaciada(linea(Element.ALIGN_LEFT, texto("Los precios ya incluyen "
                    + TextoOrden.plata(orden.getDescuentoTotal()) + " de descuento por promociones.",
                    fuente(8, Font.NORMAL, GRIS))), 2));
        }
        if (!cancelada && orden.getMillasGanadas() != null && orden.getMillasGanadas() > 0) {
            notas.add(espaciada(linea(Element.ALIGN_LEFT, texto("Sumaste " + entero(orden.getMillasGanadas())
                    + " millas con esta compra.", fuente(8, Font.NORMAL, GRIS))), 2));
        }
        PdfPCell celdaNotas = celda(Element.ALIGN_LEFT, notas.toArray(new Paragraph[0]));
        celdaNotas.setColspan(4);
        celdaNotas.setPaddingTop(8);
        celdaNotas.setPaddingLeft(10);
        tabla.addCell(celdaNotas);
        return tabla;
    }

    private PdfPCell celdaNumero(String valor, boolean negrita) {
        return celda(Element.ALIGN_RIGHT, linea(Element.ALIGN_RIGHT,
                texto(valor, fuente(9, negrita ? Font.BOLD : Font.NORMAL, AZUL_NOCHE))));
    }

    private PdfPCell filaPago(PdfPCell celda, boolean primera, boolean ultima) {
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(BORDE);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPaddingTop(7);
        celda.setPaddingBottom(8);
        celda.setPaddingLeft(primera ? 10 : 6);
        celda.setPaddingRight(ultima ? 10 : 6);
        return celda;
    }

    /** Fila de totales: rotulo en las tres primeras columnas, monto en la ultima. */
    private void filaTotal(PdfPTable tabla, String rotulo, String monto, boolean destacada, boolean primeraFila) {
        PdfPCell etiqueta = filaTotalCelda(rotulo, destacada, primeraFila);
        etiqueta.setColspan(3);
        tabla.addCell(etiqueta);
        tabla.addCell(filaTotalCelda(monto, destacada, primeraFila));
    }

    private PdfPCell filaTotalCelda(String valor, boolean destacada, boolean primeraFila) {
        Font fuente = destacada ? fuente(11, Font.BOLD, Color.WHITE) : fuente(9, Font.NORMAL, AZUL_NOCHE);
        PdfPCell celda = celda(Element.ALIGN_RIGHT, linea(Element.ALIGN_RIGHT, texto(valor, fuente)));
        celda.setPaddingTop(destacada ? 8 : (primeraFila ? 8 : 3));
        celda.setPaddingBottom(destacada ? 9 : 3);
        celda.setPaddingRight(10);
        if (destacada) {
            celda.setBackgroundColor(AZUL_NOCHE);
        }
        return celda;
    }

    // ---------- Informacion importante ----------

    private PdfPTable informacion(OrdenResponse orden) {
        boolean cancelada = orden.getEstado() == EstadoOrden.CANCELADA;
        List<String> puntos = new ArrayList<>();
        if (!cancelada) {
            if (orden.isVueloCancelado()) {
                puntos.add("La aerolínea canceló uno de los vuelos: podés cancelar la compra cuando quieras desde «Mis compras».");
            } else if (orden.getCancelableHasta() != null) {
                puntos.add("Podés cancelar la compra sin cargo hasta el " + DIA.format(orden.getCancelableHasta())
                        + " a las " + HORA.format(orden.getCancelableHasta()) + " hs desde «Mis compras».");
            }
            puntos.add("Los pases de embarque, con su código QR, están en el mail de confirmación y en «Mis compras».");
            puntos.add("Cada pasajero tiene que presentar en el aeropuerto el documento que figura en este itinerario.");
        }
        boolean hayBebes = orden.getItems().stream().anyMatch(item -> item.getTipoPasajero() == TipoPasajero.BEBE);
        if (hayBebes) {
            puntos.add("Los bebés viajan en brazos de un adulto y no ocupan asiento.");
        }
        puntos.add("Este documento es un comprobante de la compra y no es válido como factura.");

        PdfPTable tabla = tabla(new float[]{0.25f, 10});
        for (String punto : puntos) {
            // En ZapfDingbats la "l" es un circulo lleno: Helvetica no trae la vineta y la cambiaria por otra letra
            PdfPCell vineta = celda(Element.ALIGN_LEFT, linea(Element.ALIGN_LEFT,
                    texto("l", new Font(Font.ZAPFDINGBATS, 4.5f, Font.NORMAL, NARANJA))));
            vineta.setPaddingTop(1.5f);
            vineta.setPaddingBottom(5);
            tabla.addCell(vineta);
            PdfPCell texto = celda(Element.ALIGN_LEFT, linea(Element.ALIGN_LEFT, texto(punto, fuente(8.5f, Font.NORMAL, AZUL_NOCHE))));
            texto.setPaddingBottom(5);
            tabla.addCell(texto);
        }
        return tabla;
    }

    // ---------- Piezas ----------

    /** Titulo de seccion y su contenido en una sola tabla que no se corta entre paginas. */
    private PdfPTable bloque(String titulo, PdfPTable contenido) {
        PdfPTable tabla = tabla(new float[]{1});
        tabla.setKeepTogether(true);
        tabla.setSpacingBefore(ENTRE_BLOQUES);
        if (titulo != null) {
            Chunk texto = texto(titulo.toUpperCase(ARGENTINA), fuente(9, Font.BOLD, AZUL));
            texto.setCharacterSpacing(0.8f);
            PdfPCell cabecera = celda(Element.ALIGN_LEFT, linea(Element.ALIGN_LEFT, texto));
            cabecera.setBorder(Rectangle.BOTTOM);
            cabecera.setBorderColorBottom(BORDE);
            cabecera.setPaddingBottom(6);
            tabla.addCell(cabecera);
        }
        PdfPCell cuerpo = contenedor(contenido);
        cuerpo.setPaddingTop(titulo == null ? 0 : AIRE);
        tabla.addCell(cuerpo);
        return tabla;
    }

    private PdfPCell dato(String rotulo, String valor) {
        PdfPCell celda = celda(Element.ALIGN_LEFT,
                linea(Element.ALIGN_LEFT, rotulo(rotulo, GRIS)),
                espaciada(linea(Element.ALIGN_LEFT, texto(valor, fuente(9.5f, Font.BOLD, AZUL_NOCHE))), 2));
        celda.setPaddingRight(10);
        return celda;
    }

    private Chunk rotulo(String texto, Color color) {
        Chunk chunk = texto(texto.toUpperCase(ARGENTINA), fuente(6.8f, Font.BOLD, color));
        chunk.setCharacterSpacing(0.5f);
        return chunk;
    }

    private Image logo() {
        try (InputStream entrada = new ClassPathResource("mail/logo.png").getInputStream()) {
            Image logo = Image.getInstance(entrada.readAllBytes());
            logo.scaleToFit(170, 34);
            return logo;
        } catch (IOException e) {
            throw new IllegalStateException("No se encontro el logo del PDF", e);
        }
    }

    private static PdfPTable tabla(float[] anchos) {
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        return tabla;
    }

    /** Celda que contiene otra tabla, sin bordes ni relleno propios. */
    private static PdfPCell contenedor(PdfPTable tabla) {
        PdfPCell celda = sinBorde(new PdfPCell(tabla));
        celda.setPadding(0);
        return celda;
    }

    /**
     * Celda de texto. Cada renglon es un parrafo propio con su interlineado segun su letra, y la celda usa
     * el ascendente y descendente reales: asi no aparecen espacios de mas arriba ni renglones partidos.
     */
    private static PdfPCell celda(int alineacion, Paragraph... lineas) {
        PdfPCell celda = sinBorde(new PdfPCell());
        celda.setUseAscender(true);
        celda.setUseDescender(true);
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(0);
        for (Paragraph linea : lineas) {
            celda.addElement(linea);
        }
        return celda;
    }

    private static Paragraph linea(int alineacion, Chunk... partes) {
        Paragraph parrafo = new Paragraph();
        parrafo.setLeading(0, 1.25f);
        parrafo.setAlignment(alineacion);
        for (Chunk parte : partes) {
            parrafo.add(parte);
        }
        return parrafo;
    }

    private static Paragraph espaciada(Paragraph parrafo, float antes) {
        parrafo.setSpacingBefore(antes);
        return parrafo;
    }

    private static Chunk texto(String texto, Font fuente) {
        return new Chunk(texto == null || texto.isBlank() ? "-" : texto, fuente);
    }

    private static Font fuente(float tamano, int estilo, Color color) {
        return new Font(Font.HELVETICA, tamano, estilo, color);
    }

    private static PdfPCell sinBorde(PdfPCell celda) {
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }

    private static String entero(int numero) {
        return NumberFormat.getIntegerInstance(ARGENTINA).format(numero);
    }

    private static String mayuscula(String texto) {
        return texto.substring(0, 1).toUpperCase(ARGENTINA) + texto.substring(1);
    }

    /** "Sábado 10 de enero de 2027" */
    static String fechaLarga(LocalDate fecha) {
        return mayuscula(DIAS[fecha.getDayOfWeek().getValue() - 1]) + " " + fecha.getDayOfMonth() + " de "
                + MESES[fecha.getMonthValue() - 1] + " de " + fecha.getYear();
    }

    /** "Sáb 10 ene 2027" */
    static String fechaCorta(LocalDate fecha) {
        return mayuscula(DIAS_CORTOS[fecha.getDayOfWeek().getValue() - 1]) + " " + fecha.getDayOfMonth() + " "
                + MESES_CORTOS[fecha.getMonthValue() - 1] + " " + fecha.getYear();
    }

    /** Linea fina, nombre de la aerolinea, numero de orden y "Pagina X de Y" al pie de cada pagina. */
    private static final class PieDePagina extends PdfPageEventHelper {

        private static final float TAMANO = 7.5f;
        private final String texto;
        private PdfTemplate totalPaginas;
        private BaseFont letra;

        PieDePagina(Long ordenId) {
            this.texto = "BCA Airlines  ·  Orden de compra N° " + ordenId;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document documento) {
            totalPaginas = writer.getDirectContent().createTemplate(30, 12);
            try {
                letra = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo cargar la letra del pie", e);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document documento) {
            PdfContentByte lienzo = writer.getDirectContent();
            float y = documento.bottom() - 30;
            lienzo.saveState();
            lienzo.setColorStroke(BORDE);
            lienzo.setLineWidth(0.6f);
            lienzo.moveTo(documento.left(), y + 11);
            lienzo.lineTo(documento.right(), y + 11);
            lienzo.stroke();

            String pagina = "Página " + writer.getPageNumber() + " de ";
            float anchoTotal = letra.getWidthPoint("9", TAMANO);
            lienzo.beginText();
            lienzo.setFontAndSize(letra, TAMANO);
            lienzo.setColorFill(GRIS);
            lienzo.showTextAligned(PdfContentByte.ALIGN_LEFT, texto, documento.left(), y, 0);
            lienzo.showTextAligned(PdfContentByte.ALIGN_RIGHT, pagina, documento.right() - anchoTotal, y, 0);
            lienzo.endText();
            lienzo.addTemplate(totalPaginas, documento.right() - anchoTotal, y);
            lienzo.restoreState();
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document documento) {
            totalPaginas.beginText();
            totalPaginas.setFontAndSize(letra, TAMANO);
            totalPaginas.setColorFill(GRIS);
            totalPaginas.setTextMatrix(0, 0);
            totalPaginas.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPaginas.endText();
        }
    }
}
