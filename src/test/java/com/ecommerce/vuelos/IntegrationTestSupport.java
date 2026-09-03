package com.ecommerce.vuelos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String loginAdmin() throws Exception {
        return login("admin", "admin123");
    }

    protected String registrarYLoguearComprador(String username) throws Exception {
        Map<String, String> body = Map.of(
                "username", username,
                "mail", username + "@test.com",
                "password", "123456",
                "nombre", "Test",
                "apellido", "Comprador",
                "rol", "COMPRADOR"
        );
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
        return login(username, "123456");
    }

    protected String registrarYLoguearVendedor(String username) throws Exception {
        Map<String, String> body = Map.of(
                "username", username,
                "mail", username + "@test.com",
                "password", "123456",
                "nombre", "Test",
                "apellido", "Vendedor",
                "rol", "VENDEDOR"
        );
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
        return login(username, "123456");
    }

    /** El seeder deja siempre una categoria "Cabotaje" y los aeropuertos EZE/MAD/AEP/COR. */
    protected Long categoriaPorDefecto() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/vuelos"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get(0).get("categoriaId").asLong();
    }

    protected String login(String username, String password) throws Exception {
        Map<String, String> body = Map.of("username", username, "password", password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    protected Long crearVuelo(String vendedorToken, String origenIata, String destinoIata,
                              double precio, int asientos, String clase) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("numeroVuelo", "TS" + System.nanoTime() % 10000);
        body.put("descripcion", "Vuelo de prueba");
        body.put("categoriaId", categoriaPorDefecto());
        body.put("origenIata", origenIata);
        body.put("destinoIata", destinoIata);
        body.put("fechaSalida", LocalDateTime.now().plusDays(10).withNano(0).toString());
        body.put("fechaLlegada", LocalDateTime.now().plusDays(10).plusHours(3).withNano(0).toString());
        body.put("precio", precio);
        body.put("asientosDisponibles", asientos);
        body.put("descuento", 0);
        body.put("clase", clase);

        MvcResult result = mockMvc.perform(post("/api/vuelos")
                        .header("Authorization", "Bearer " + vendedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return idDe(result);
    }

    protected Long agregarAlCarrito(String pasajeroToken, Long vueloId, int cantidad) throws Exception {
        Map<String, Object> body = Map.of("vueloId", vueloId, "cantidad", cantidad);
        MvcResult result = mockMvc.perform(post("/api/carrito/items")
                        .header("Authorization", "Bearer " + pasajeroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("items");
        return items.get(items.size() - 1).get("id").asLong();
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    protected Long idDe(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
