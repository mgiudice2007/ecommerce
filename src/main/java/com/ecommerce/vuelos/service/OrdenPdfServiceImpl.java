package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.orden.ItemOrdenResponse;
import com.ecommerce.vuelos.dto.orden.OrdenResponse;
import com.ecommerce.vuelos.dto.orden.PasajeResponse;
import com.ecommerce.vuelos.entity.EstadoOrden;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class OrdenPdfServiceImpl implements OrdenPdfService {

    // Mismos colores que el front y el mail
    private static final Color AZUL = new Color(0x00, 0x57, 0xB8);
    private static final Color AZUL_NOCHE = new Color(0x04, 0x1E, 0x42);
    private static final Color NARANJA = new Color(0xF2, 0x60, 0x0C);
    private static final Color GRIS = new Color(0x5B, 0x6B, 0x7B);
    private static final Color CELESTE = new Color(0xE8, 0xF1, 0xF9);
    private static final Color BORDE = new Color(0xDD, 0xE3, 0xEA);
    private static final Color ROJO = new Color(0xB9, 0x1C, 0x1C);

    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, AZUL_NOCHE);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD, AZUL);
    private static final Font ETIQUETA = new Font(Font.HELVETICA, 7.5f, Font.BOLD, GRIS);
    private static final Font TEXTO = new Font(Font.HELVETICA, 9.5f, Font.NORMAL, AZUL_NOCHE);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 9.5f, Font.BOLD, AZUL_NOCHE);
    private static final Font CABECERA = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.WHITE);
    private static final Font CODIGO = new Font(Font.COURIER, 10, Font.BOLD, AZUL);
    private static final Font TOTAL = new Font(Font.HELVETICA, 14, Font.BOLD, AZUL);
    private static final Font CHICO = new Font(Font.HELVETICA, 8, Font.NORMAL, GRIS);

    @Override
    public byte[] ordenDeCompra(OrdenResponse orden, String nombreCliente, String emailCliente) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 36, 36);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        agregarEncabezado(documento, orden);
        agregarDatos(documento, orden, nombreCliente, emailCliente);
        agregarVuelos(documento, orden);
        agregarTotales(documento, orden);
        agregarPasajeros(documento, orden);
        agregarPie(documento, orden);

        documento.close();
        return salida.toByteArray();
    }

    /** Logo a la izquierda; numero de orden y estado a la derecha; linea naranja abajo. */
    private void agregarEncabezado(Document documento, OrdenResponse orden) {
        PdfPTable tabla = new PdfPTable(new float[]{1, 1});
        tabla.setWidthPercentage(100);

        PdfPCell logo = sinBorde(new PdfPCell(logo()));
        logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(logo);

        Paragraph derecha = new Paragraph();
        derecha.setAlignment(Element.ALIGN_RIGHT);
        derecha.add(new Phrase("ORDEN DE COMPRA\n", ETIQUETA));
        derecha.add(new Phrase("N° " + orden.getId() + "\n", TITULO));
        boolean cancelada = orden.getEstado() == EstadoOrden.CANCELADA;
        derecha.add(new Phrase(cancelada ? "CANCELADA" : "CONFIRMADA",
                new Font(Font.HELVETICA, 9, Font.BOLD, cancelada ? ROJO : NARANJA)));
        PdfPCell celda = sinBorde(new PdfPCell());
        celda.addElement(derecha);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(celda);

        PdfPCell linea = sinBorde(new PdfPCell());
        linea.setColspan(2);
        linea.setBorder(Rectangle.BOTTOM);
        linea.setBorderColorBottom(NARANJA);
        linea.setBorderWidthBottom(2.5f);
        linea.setFixedHeight(10);
        tabla.addCell(linea);
        tabla.setSpacingAfter(16);
        documento.add(tabla);
    }

    /** Franja celeste con el cliente, la fecha y el medio de pago. */
    private void agregarDatos(Document documento, OrdenResponse orden, String nombreCliente, String emailCliente) {
        PdfPTable tabla = new PdfPTable(new float[]{1.3f, 1, 1.3f});
        tabla.setWidthPercentage(100);
        tabla.addCell(dato("CLIENTE", nombreCliente + "\n" + emailCliente));
        tabla.addCell(dato("FECHA DE COMPRA", TextoOrden.FECHA.format(orden.getFecha()) + " hs"));
        tabla.addCell(dato("MEDIO DE PAGO", TextoOrden.medioPago(orden)));
        tabla.setSpacingAfter(18);
        documento.add(tabla);
    }

    private void agregarVuelos(Document documento, OrdenResponse orden) {
        documento.add(subtitulo("Detalle de la compra"));
        PdfPTable tabla = new PdfPTable(new float[]{0.9f, 2.1f, 1.2f, 1.25f, 1.1f, 1.25f, 1.25f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (String titulo : new String[]{"Vuelo", "Tramo", "Salida", "Clase", "Pasajeros", "Precio unit.", "Subtotal"}) {
            tabla.addCell(cabecera(titulo));
        }
        for (ItemOrdenResponse item : orden.getItems()) {
            tabla.addCell(celda(item.getNumeroVuelo(), NEGRITA, Element.ALIGN_LEFT));
            tabla.addCell(celda(item.getOrigen() + " – " + item.getDestino(), TEXTO, Element.ALIGN_LEFT));
            tabla.addCell(celda(item.getFechaSalida() == null ? "-" : TextoOrden.FECHA.format(item.getFechaSalida()),
                    TEXTO, Element.ALIGN_LEFT));
            tabla.addCell(celda(item.getClaseNombre(), TEXTO, Element.ALIGN_LEFT));
            tabla.addCell(celda(item.getCantidad() + " × " + TextoOrden.tipoPasajero(item.getTipoPasajero()),
                    TEXTO, Element.ALIGN_LEFT));
            tabla.addCell(celda(TextoOrden.plata(item.getPrecioUnitario()), TEXTO, Element.ALIGN_RIGHT));
            tabla.addCell(celda(TextoOrden.plata(item.getSubtotal()), NEGRITA, Element.ALIGN_RIGHT));
        }
        documento.add(tabla);
    }

    private void agregarTotales(Document documento, OrdenResponse orden) {
        PdfPTable tabla = new PdfPTable(new float[]{3, 1.4f});
        tabla.setWidthPercentage(55);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setSpacingBefore(8);
        if (orden.getMillasUsadas() != null && orden.getMillasUsadas() > 0) {
            tabla.addCell(fila("Descuento por " + orden.getMillasUsadas() + " millas", TEXTO));
            tabla.addCell(fila("- " + TextoOrden.plata(orden.getDescuentoMillas()), TEXTO));
        }
        PdfPCell etiqueta = fila("TOTAL", TOTAL);
        PdfPCell total = fila(TextoOrden.plata(orden.getTotal()), TOTAL);
        for (PdfPCell c : new PdfPCell[]{etiqueta, total}) {
            c.setBorder(Rectangle.TOP);
            c.setBorderColorTop(AZUL);
            c.setBorderWidthTop(1.5f);
            c.setPaddingTop(6);
            tabla.addCell(c);
        }
        if (orden.getMillasGanadas() != null && orden.getMillasGanadas() > 0) {
            PdfPCell millas = fila("Sumaste " + orden.getMillasGanadas() + " millas con esta compra.", CHICO);
            millas.setColspan(2);
            tabla.addCell(millas);
        }
        tabla.setSpacingAfter(18);
        documento.add(tabla);
    }

    private void agregarPasajeros(Document documento, OrdenResponse orden) {
        PdfPTable tabla = new PdfPTable(new float[]{1.9f, 1.4f, 0.9f, 1.7f, 1.1f, 1.7f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (String titulo : new String[]{"Pasajero", "Documento", "Vuelo", "Tramo", "Asiento", "Código de embarque"}) {
            tabla.addCell(cabecera(titulo));
        }
        boolean hayPasajes = false;
        for (ItemOrdenResponse item : orden.getItems()) {
            if (item.getPasajes() == null) {
                continue;
            }
            for (PasajeResponse pasaje : item.getPasajes()) {
                hayPasajes = true;
                tabla.addCell(celda(pasaje.getNombre() + " " + pasaje.getApellido(), NEGRITA, Element.ALIGN_LEFT));
                String documentoPasajero = pasaje.getTipoDocumento() == null
                        ? pasaje.getDocumento()
                        : pasaje.getTipoDocumento() + " " + pasaje.getDocumento();
                tabla.addCell(celda(documentoPasajero, TEXTO, Element.ALIGN_LEFT));
                tabla.addCell(celda(item.getNumeroVuelo(), TEXTO, Element.ALIGN_LEFT));
                tabla.addCell(celda(item.getOrigen() + " – " + item.getDestino(), TEXTO, Element.ALIGN_LEFT));
                tabla.addCell(celda(pasaje.getAsiento() == null ? "En brazos" : pasaje.getAsiento(), TEXTO, Element.ALIGN_LEFT));
                tabla.addCell(celda(pasaje.getCodigoEmbarque(), CODIGO, Element.ALIGN_LEFT));
            }
        }
        if (hayPasajes) {
            documento.add(subtitulo("Pasajeros"));
            documento.add(tabla);
        }
    }

    private void agregarPie(Document documento, OrdenResponse orden) {
        Paragraph pie = new Paragraph();
        pie.setSpacingBefore(22);
        if (orden.getCancelableHasta() != null && orden.getEstado() != EstadoOrden.CANCELADA) {
            pie.add(new Phrase("Podés cancelar la compra hasta el " + TextoOrden.FECHA.format(orden.getCancelableHasta())
                    + " hs desde «Mis compras».\n", CHICO));
        }
        pie.add(new Phrase("Los pases de embarque con su QR están en el mail de confirmación y en «Mis compras». "
                + "Este documento es un comprobante de la compra y no es válido como factura.", CHICO));
        documento.add(pie);
    }

    // ---------- Piezas ----------

    private Image logo() {
        try (InputStream entrada = new ClassPathResource("mail/logo.png").getInputStream()) {
            Image logo = Image.getInstance(entrada.readAllBytes());
            logo.scaleToFit(180, 36);
            return logo;
        } catch (IOException e) {
            throw new IllegalStateException("No se encontro el logo del PDF", e);
        }
    }

    private Paragraph subtitulo(String texto) {
        Paragraph parrafo = new Paragraph(texto, SUBTITULO);
        parrafo.setSpacingAfter(6);
        return parrafo;
    }

    private PdfPCell dato(String etiqueta, String valor) {
        Paragraph parrafo = new Paragraph();
        parrafo.add(new Phrase(etiqueta + "\n", ETIQUETA));
        parrafo.add(new Phrase(valor, NEGRITA));
        PdfPCell celda = sinBorde(new PdfPCell());
        celda.addElement(parrafo);
        celda.setBackgroundColor(CELESTE);
        celda.setPadding(9);
        celda.setPaddingTop(4);
        return celda;
    }

    private PdfPCell cabecera(String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, CABECERA));
        celda.setBackgroundColor(AZUL_NOCHE);
        celda.setBorderColor(AZUL_NOCHE);
        celda.setPadding(6);
        return celda;
    }

    private PdfPCell celda(String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "-" : texto, fuente));
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColorBottom(BORDE);
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(6);
        return celda;
    }

    private PdfPCell fila(String texto, Font fuente) {
        PdfPCell celda = sinBorde(new PdfPCell(new Phrase(texto, fuente)));
        celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celda.setPadding(3);
        return celda;
    }

    private PdfPCell sinBorde(PdfPCell celda) {
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }
}
