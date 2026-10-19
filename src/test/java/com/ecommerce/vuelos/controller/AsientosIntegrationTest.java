package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AsientosIntegrationTest extends IntegrationTestSupport {

    private JsonNode mapa(Long disponibilidadId) throws Exception {
        String json = mockMvc.perform(get("/api/disponibilidades/" + disponibilidadId + "/asientos"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    /** Los primeros asientos del mapa, en orden. */
    private List<String> primerosAsientos(Long disponibilidadId, int cantidad) throws Exception {
        List<String> codigos = new ArrayList<>();
        for (JsonNode fila : mapa(disponibilidadId).get("filas")) {
            for (JsonNode asiento : fila.get("asientos")) {
                if (codigos.size() < cantidad) {
                    codigos.add(asiento.asText());
                }
            }
        }
        return codigos;
    }

    private List<String> ocupados(Long disponibilidadId) throws Exception {
        List<String> ocupados = new ArrayList<>();
        mapa(disponibilidadId).get("ocupados").forEach(codigo -> ocupados.add(codigo.asText()));
        return ocupados;
    }

    private void elegir(String token, Long disponibilidadId, List<String> asientos, int statusEsperado) throws Exception {
        mockMvc.perform(json(put("/api/carrito/asientos").header("Authorization", "Bearer " + token),
                        Map.of("disponibilidadId", disponibilidadId, "asientos", asientos)))
                .andExpect(status().is(statusEsperado));
    }

    private Long comprar(String token) throws Exception {
        String json = mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    @Test
    void elMapa_tieneFilasSegunLosAsientosPorFilaDeLaClase() throws Exception {
        String admin = registrarYLoguearAdmin("vmapa");
        Long disponibilidadId = crearVueloConCupo(admin, "EZE", "MIA", 1000.0, 10);

        JsonNode mapa = mapa(disponibilidadId);
        int porFila = mapa.get("letras").size();
        int asientos = 0;
        for (JsonNode fila : mapa.get("filas")) {
            assertTrue(fila.get("asientos").size() <= porFila);
            asientos += fila.get("asientos").size();
        }
        assertEquals(10, asientos, "el mapa tiene un asiento por cada lugar del cupo");
        assertTrue(mapa.get("ocupados").isEmpty());
    }

    @Test
    void elegirAsientosYComprar_losDejaOcupados() throws Exception {
        String admin = registrarYLoguearAdmin("vasientos");
        Long disponibilidadId = crearVueloConCupo(admin, "EZE", "MIA", 1000.0, 10);
        String comprador = registrarYLoguearComprador("eligeasientos");
        List<String> elegidos = primerosAsientos(disponibilidadId, 2);

        agregarAlCarrito(comprador, disponibilidadId, 2);
        elegir(comprador, disponibilidadId, elegidos, 200);
        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + comprador))
                .andExpect(jsonPath("$.items[0].asientos[0]").value(elegidos.get(0)))
                .andExpect(jsonPath("$.items[0].asientos[1]").value(elegidos.get(1)));

        Long ordenId = comprar(comprador);
        mockMvc.perform(get("/api/ordenes/" + ordenId).header("Authorization", "Bearer " + comprador))
                .andExpect(jsonPath("$.items[0].asientos.length()").value(2));
        assertTrue(ocupados(disponibilidadId).containsAll(elegidos));
    }

    @Test
    void agregarPasajesConAsientos_losGuardaEnElCarrito() throws Exception {
        String admin = registrarYLoguearAdmin("vpasajesasientos");
        Long disponibilidadId = crearVueloConCupo(admin, "EZE", "MIA", 1000.0, 10);
        String comprador = registrarYLoguearComprador("eligeantes");
        List<String> elegidos = primerosAsientos(disponibilidadId, 3);

        // 2 adultos y 1 niño con sus asientos elegidos antes de agregar al carrito
        mockMvc.perform(json(post("/api/carrito/pasajes").header("Authorization", "Bearer " + comprador),
                        Map.of("disponibilidadId", disponibilidadId, "adultos", 2, "ninos", 1, "bebes", 0,
                                "asientos", elegidos)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[?(@.tipoPasajero == 'ADULTO')].asientos[0]").value(elegidos.get(0)))
                .andExpect(jsonPath("$.items[?(@.tipoPasajero == 'NINO')].asientos[0]").value(elegidos.get(2)));
    }

    @Test
    void elegirUnAsientoYaVendido_devuelve400() throws Exception {
        String admin = registrarYLoguearAdmin("vocupado");
        Long disponibilidadId = crearVueloConCupo(admin, "EZE", "MIA", 1000.0, 10);
        List<String> primero = primerosAsientos(disponibilidadId, 1);

        String uno = registrarYLoguearComprador("primeroenllegar");
        agregarAlCarrito(uno, disponibilidadId, 1);
        elegir(uno, disponibilidadId, primero, 200);
        comprar(uno);

        String dos = registrarYLoguearComprador("llegotarde");
        agregarAlCarrito(dos, disponibilidadId, 1);
        elegir(dos, disponibilidadId, primero, 400);
    }

    @Test
    void elegirMasAsientosQuePasajeros_oUnoQueNoExiste_devuelve400() throws Exception {
        String admin = registrarYLoguearAdmin("vdemas");
        Long disponibilidadId = crearVueloConCupo(admin, "EZE", "MIA", 1000.0, 10);
        String comprador = registrarYLoguearComprador("demasiados");

        agregarAlCarrito(comprador, disponibilidadId, 1);
        elegir(comprador, disponibilidadId, primerosAsientos(disponibilidadId, 2), 400);
        elegir(comprador, disponibilidadId, List.of("99Z"), 400);
    }

    @Test
    void comprarSinElegir_leAsignaAsientosLibres() throws Exception {
        String admin = registrarYLoguearAdmin("vautomatico");
        Long disponibilidadId = crearVueloConCupo(admin, "EZE", "MIA", 1000.0, 10);
        String comprador = registrarYLoguearComprador("sineligir");

        agregarAlCarrito(comprador, disponibilidadId, 3);
        Long ordenId = comprar(comprador);

        mockMvc.perform(get("/api/ordenes/" + ordenId).header("Authorization", "Bearer " + comprador))
                .andExpect(jsonPath("$.items[0].asientos.length()").value(3));
        assertEquals(3, ocupados(disponibilidadId).size());
    }

    @Test
    void cancelarLaCompra_liberaLosAsientos() throws Exception {
        String admin = registrarYLoguearAdmin("vlibera");
        Long disponibilidadId = crearVueloConCupo(admin, "EZE", "MIA", 1000.0, 10);
        String comprador = registrarYLoguearComprador("cancelaasientos");

        agregarAlCarrito(comprador, disponibilidadId, 2);
        Long ordenId = comprar(comprador);
        assertEquals(2, ocupados(disponibilidadId).size());

        mockMvc.perform(post("/api/ordenes/" + ordenId + "/cancelar").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());
        assertTrue(ocupados(disponibilidadId).isEmpty());
    }

    @Test
    void losBebes_viajanAUpaYNoOcupanAsiento() throws Exception {
        String admin = registrarYLoguearAdmin("vbebe");
        Long disponibilidadId = crearVueloConCupo(admin, "EZE", "MIA", 1000.0, 1);
        String comprador = registrarYLoguearComprador("conbebe");

        // Con un solo asiento libre entran un adulto y su bebé
        mockMvc.perform(json(post("/api/carrito/pasajes").header("Authorization", "Bearer " + comprador),
                        Map.of("disponibilidadId", disponibilidadId, "adultos", 1, "ninos", 0, "bebes", 1)))
                .andExpect(status().isCreated());
        comprar(comprador);

        mockMvc.perform(get("/api/disponibilidades/" + disponibilidadId))
                .andExpect(jsonPath("$.asientosDisponibles").value(0));
        assertEquals(1, ocupados(disponibilidadId).size());
    }
}
