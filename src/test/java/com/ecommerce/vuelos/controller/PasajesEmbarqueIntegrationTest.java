package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasajesEmbarqueIntegrationTest extends IntegrationTestSupport {

    private Map<String, Object> pasajero(Long itemId, String nombre, String documento, LocalDate nacimiento) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("itemId", itemId);
        datos.put("nombre", nombre);
        datos.put("apellido", "Gómez");
        datos.put("tipoDocumento", "DNI");
        datos.put("documento", documento);
        datos.put("fechaNacimiento", nacimiento.toString());
        return datos;
    }

    private Map<String, Object> conPasajeros(Map<String, Object>... pasajeros) {
        return Map.of("pasajeros", List.of(pasajeros));
    }

    @SafeVarargs
    private void checkoutDebeFallarCon(String token, String mensaje, Map<String, Object>... pasajeros) throws Exception {
        mockMvc.perform(json(post("/api/carrito/checkout").header("Authorization", "Bearer " + token),
                        conPasajeros(pasajeros)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString(mensaje)));
    }

    private JsonNode comprarConPasajeros(String comprador) throws Exception {
        String respuesta = mockMvc.perform(checkout(comprador))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta);
    }

    @Test
    void checkout_sinDatosDePasajeros_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 200.0, 10);
        String comprador = registrarYLoguearComprador("sinpasajeros");
        agregarAlCarrito(comprador, cupoId, 1);

        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("datos de los pasajeros")));
    }

    @Test
    void checkout_conMenosPasajerosQueLugares_devuelve400_yNoDescuentaStock() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 200.0, 10);
        String comprador = registrarYLoguearComprador("faltaunopasajero");
        Long itemId = agregarAlCarrito(comprador, cupoId, 2);

        checkoutDebeFallarCon(comprador, "Faltan datos de pasajeros",
                pasajero(itemId, "Ana", "30111222", LocalDate.now().minusYears(30)));

        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + comprador))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void checkout_conAdultoQueEsUnNino_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 200.0, 10);
        String comprador = registrarYLoguearComprador("adultochico");
        Long itemId = agregarAlCarrito(comprador, cupoId, 1);

        checkoutDebeFallarCon(comprador, "tendría 5 años",
                pasajero(itemId, "Tomás", "30111222", LocalDate.now().minusYears(5)));
    }

    @Test
    void checkout_conDocumentoRepetidoEnElMismoVuelo_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 200.0, 10);
        String comprador = registrarYLoguearComprador("docrepetido");
        Long itemId = agregarAlCarrito(comprador, cupoId, 2);

        checkoutDebeFallarCon(comprador, "repetido",
                pasajero(itemId, "Ana", "30111222", LocalDate.now().minusYears(30)),
                pasajero(itemId, "Luis", "30111222", LocalDate.now().minusYears(31)));
    }

    @Test
    void checkout_conDniInvalido_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 200.0, 10);
        String comprador = registrarYLoguearComprador("dniinvalido");
        Long itemId = agregarAlCarrito(comprador, cupoId, 1);

        checkoutDebeFallarCon(comprador, "DNI", pasajero(itemId, "Ana", "ABC12345", LocalDate.now().minusYears(30)));
    }

    @Test
    void checkout_conPasajerosCorrectos_generaUnPasajeConCodigoYAsientoPorPersona() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 200.0, 10);
        String comprador = registrarYLoguearComprador("dospasajeros");
        Long itemId = agregarAlCarrito(comprador, cupoId, 2);

        String respuesta = mockMvc.perform(json(post("/api/carrito/checkout").header("Authorization", "Bearer " + comprador),
                        conPasajeros(
                                pasajero(itemId, "Ana", "30111222", LocalDate.now().minusYears(30)),
                                pasajero(itemId, "Luis", "28111222", LocalDate.now().minusYears(35)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].pasajes.length()").value(2))
                .andExpect(jsonPath("$.items[0].pasajes[0].nombre").value("Ana"))
                .andExpect(jsonPath("$.items[0].pasajes[1].nombre").value("Luis"))
                .andReturn().getResponse().getContentAsString();

        JsonNode pasajes = objectMapper.readTree(respuesta).get("items").get(0).get("pasajes");
        List<String> codigos = new ArrayList<>();
        for (JsonNode pasaje : pasajes) {
            assertTrue(pasaje.get("codigoEmbarque").asText().matches("BCA-[A-Z2-9]{8}"));
            assertTrue(pasaje.get("asiento").asText().matches("\\d+[A-F]"));
            codigos.add(pasaje.get("codigoEmbarque").asText());
        }
        assertNotEquals(codigos.get(0), codigos.get(1));
        // cada persona tiene su propio asiento, el mismo que figura en la lista de asientos del item
        assertNotEquals(pasajes.get(0).get("asiento").asText(), pasajes.get(1).get("asiento").asText());
    }

    @Test
    void validarYEmbarcar_flujoCompletoDelAdministrador() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 200.0, 10);
        String comprador = registrarYLoguearComprador("aembarcar");
        agregarAlCarrito(comprador, cupoId, 1);
        String codigo = comprarConPasajeros(comprador).get("items").get(0).get("pasajes").get(0)
                .get("codigoEmbarque").asText();

        mockMvc.perform(get("/api/embarque/" + codigo.toLowerCase()).header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDO"))
                .andExpect(jsonPath("$.codigoEmbarque").value(codigo))
                .andExpect(jsonPath("$.nombre").value("Pasajero"));

        mockMvc.perform(post("/api/embarque/" + codigo + "/embarcar").header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EMBARCADO"));

        mockMvc.perform(get("/api/embarque/" + codigo).header("Authorization", "Bearer " + vendedor))
                .andExpect(jsonPath("$.estado").value("EMBARCADO"))
                .andExpect(jsonPath("$.motivo").value(org.hamcrest.Matchers.containsString("ya embarcó")));

        // no se puede embarcar dos veces
        mockMvc.perform(post("/api/embarque/" + codigo + "/embarcar").header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compraCancelada_dejaElPasajeAnulado_yNoSePuedeEmbarcar() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 200.0, 10);
        String comprador = registrarYLoguearComprador("cancelaytiene");
        agregarAlCarrito(comprador, cupoId, 1);
        JsonNode orden = comprarConPasajeros(comprador);
        String codigo = orden.get("items").get(0).get("pasajes").get(0).get("codigoEmbarque").asText();

        mockMvc.perform(post("/api/ordenes/" + orden.get("id").asLong() + "/cancelar")
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/embarque/" + codigo).header("Authorization", "Bearer " + vendedor))
                .andExpect(jsonPath("$.estado").value("ANULADO"))
                .andExpect(jsonPath("$.motivo").value("la compra fue cancelada"));
        mockMvc.perform(post("/api/embarque/" + codigo + "/embarcar").header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isBadRequest());

        // el pasaje conserva su asiento y sus datos en el historial aunque la compra este cancelada
        mockMvc.perform(get("/api/ordenes/" + orden.get("id").asLong()).header("Authorization", "Bearer " + comprador))
                .andExpect(jsonPath("$.items[0].pasajes[0].codigoEmbarque").value(codigo));
    }

    @Test
    void embarque_soloParaElAdministrador_yCodigoInexistenteDa404() throws Exception {
        String vendedor = registrarYLoguearAdmin("vend" + System.nanoTime() % 100000);
        String comprador = registrarYLoguearComprador("sinpermiso");

        mockMvc.perform(get("/api/embarque/BCA-AAAAAAAA").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/embarque/BCA-AAAAAAAA"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/embarque/BCA-AAAAAAAA").header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isNotFound());
    }
}
