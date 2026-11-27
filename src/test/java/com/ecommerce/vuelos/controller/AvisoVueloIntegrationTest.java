package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import com.ecommerce.vuelos.event.VueloModificadoEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Avisos por mail a los compradores cuando el vuelo se demora, se cancela, vuelve a operar o cambia de horario. */
@RecordApplicationEvents
class AvisoVueloIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationEvents eventos;

    private String sufijo() {
        return String.valueOf(System.nanoTime() % 100000);
    }

    private Long comprar(String comprador, Long cupoId, int cantidad) throws Exception {
        agregarAlCarrito(comprador, cupoId, cantidad);
        String orden = mockMvc.perform(checkout(comprador))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(orden).get("id").asLong();
    }

    private ResultActions cambiarEstado(String vendedor, Long vueloId, String estado) throws Exception {
        return mockMvc.perform(json(patch("/api/vuelos/" + vueloId + "/estado").header("Authorization", "Bearer " + vendedor),
                Map.of("estado", estado)));
    }

    private Map<String, Object> datosDelVuelo(LocalDateTime salida, String descripcion) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("numeroVuelo", "AV" + sufijo());
        body.put("descripcion", descripcion);
        body.put("categoriaId", categoriaPorDefecto());
        body.put("origenIata", "EZE");
        body.put("destinoIata", "MAD");
        body.put("fechaSalida", salida.toString());
        body.put("fechaLlegada", salida.plusHours(3).toString());
        return body;
    }

    private List<VueloModificadoEvent> avisos() {
        return eventos.stream(VueloModificadoEvent.class).toList();
    }

    @Test
    void demorarUnVueloConVentas_avisaUnaVezACadaComprador() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + sufijo());
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 100.0);
        String ana = "avisoana" + sufijo();
        String tokenAna = registrarYLoguearComprador(ana);
        comprar(tokenAna, cupoId, 1);
        comprar(tokenAna, cupoId, 1); // dos compras del mismo comprador: un solo mail
        String luis = "avisoluis" + sufijo();
        comprar(registrarYLoguearComprador(luis), cupoId, 2);

        cambiarEstado(vendedor, vueloId, "DEMORADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(containsString("Se aviso por mail a 2 compradores")));

        List<VueloModificadoEvent> avisos = avisos();
        assertEquals(1, avisos.size());
        VueloModificadoEvent aviso = avisos.get(0);
        assertEquals(VueloModificadoEvent.Motivo.DEMORADO, aviso.motivo());
        assertEquals(2, aviso.compradores().size());
        List<String> mails = aviso.compradores().stream().map(VueloModificadoEvent.Comprador::mail).toList();
        assertTrue(mails.contains(ana + "@test.com"));
        assertTrue(mails.contains(luis + "@test.com"));
        assertNull(aviso.salidaAnterior());
    }

    @Test
    void volverAActivoDespuesDeLaDemora_avisaQueOperaNormal() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + sufijo());
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 100.0);
        comprar(registrarYLoguearComprador("avisonormal" + sufijo()), cupoId, 1);

        cambiarEstado(vendedor, vueloId, "DEMORADO").andExpect(status().isOk());
        cambiarEstado(vendedor, vueloId, "ACTIVO").andExpect(status().isOk());

        List<VueloModificadoEvent> avisos = avisos();
        assertEquals(2, avisos.size());
        assertEquals(VueloModificadoEvent.Motivo.NORMALIZADO, avisos.get(1).motivo());
    }

    @Test
    void pausarUnVuelo_noLeAvisaANadie() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + sufijo());
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 100.0);
        comprar(registrarYLoguearComprador("avisopausa" + sufijo()), cupoId, 1);

        // Pausar solo frena la venta; reactivarlo despues tampoco es una novedad para el que compro
        cambiarEstado(vendedor, vueloId, "PAUSADO").andExpect(status().isOk());
        cambiarEstado(vendedor, vueloId, "ACTIVO").andExpect(status().isOk());

        assertTrue(avisos().isEmpty());
    }

    @Test
    void demorarUnVueloSinVentas_noAvisa() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + sufijo());
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0);
        crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 100.0);

        cambiarEstado(vendedor, vueloId, "DEMORADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(not(containsString("Se aviso"))));

        assertTrue(avisos().isEmpty());
    }

    @Test
    void unaCompraCancelada_noRecibeElAviso() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + sufijo());
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 100.0);
        String comprador = registrarYLoguearComprador("avisocancelo" + sufijo());
        Long ordenId = comprar(comprador, cupoId, 1);
        mockMvc.perform(post("/api/ordenes/" + ordenId + "/cancelar").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        cambiarEstado(vendedor, vueloId, "CANCELADO").andExpect(status().isOk());

        assertTrue(avisos().isEmpty());
    }

    @Test
    void unVueloQueYaAterrizo_noAvisa() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + sufijo());
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 100.0);
        comprar(registrarYLoguearComprador("avisoviejo" + sufijo()), cupoId, 1);
        hacerQueYaSalio(vueloId);

        cambiarEstado(vendedor, vueloId, "DEMORADO").andExpect(status().isOk());

        assertTrue(avisos().isEmpty());
    }

    @Test
    void reprogramarElVuelo_avisaConElHorarioAnteriorYElNuevo() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + sufijo());
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 100.0);
        comprar(registrarYLoguearComprador("avisoreprog" + sufijo()), cupoId, 1);
        String vuelo = mockMvc.perform(get("/api/vuelos/" + vueloId)).andReturn().getResponse().getContentAsString();
        LocalDateTime salidaOriginal = LocalDateTime.parse(objectMapper.readTree(vuelo).get("fechaSalida").asText());

        // Cambiar solo la descripcion no es una novedad para el que compro
        mockMvc.perform(json(put("/api/vuelos/" + vueloId).header("Authorization", "Bearer " + vendedor),
                        datosDelVuelo(salidaOriginal, "Otra descripcion")))
                .andExpect(status().isOk());
        assertTrue(avisos().isEmpty());

        LocalDateTime nuevaSalida = salidaOriginal.plusHours(5);
        mockMvc.perform(json(put("/api/vuelos/" + vueloId).header("Authorization", "Bearer " + vendedor),
                        datosDelVuelo(nuevaSalida, "Otra descripcion")))
                .andExpect(status().isOk());

        List<VueloModificadoEvent> avisos = avisos();
        assertEquals(1, avisos.size());
        VueloModificadoEvent aviso = avisos.get(0);
        assertEquals(VueloModificadoEvent.Motivo.REPROGRAMADO, aviso.motivo());
        assertEquals(salidaOriginal, aviso.salidaAnterior());
        assertEquals(nuevaSalida, aviso.salida());
        assertEquals(1, aviso.compradores().size());
    }

    @Test
    void siLaAerolineaCancelaElVuelo_laCompraSePuedeCancelarAunqueFaltenMenosDe24Horas() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + sufijo());
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 100.0);
        String comprador = registrarYLoguearComprador("avisocancelado" + sufijo());
        Long ordenId = comprar(comprador, cupoId, 1);
        hacerQueSalgaEn(vueloId, 10);

        cambiarEstado(vendedor, vueloId, "CANCELADO").andExpect(status().isOk());
        assertEquals(VueloModificadoEvent.Motivo.CANCELADO, avisos().get(0).motivo());

        mockMvc.perform(get("/api/ordenes/" + ordenId).header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vueloCancelado").value(true))
                .andExpect(jsonPath("$.sePuedeCancelar").value(true));
        mockMvc.perform(post("/api/ordenes/" + ordenId + "/cancelar").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void eliminarUnVueloYaCancelado_noVuelveAAvisar() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + sufijo());
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 100.0);
        comprar(registrarYLoguearComprador("avisoelim" + sufijo()), cupoId, 1);

        cambiarEstado(vendedor, vueloId, "CANCELADO").andExpect(status().isOk());
        cambiarEstado(vendedor, vueloId, "ELIMINADO").andExpect(status().isOk());

        assertEquals(1, avisos().size());
    }
}
