package com.ecommerce.vuelos;

import com.ecommerce.vuelos.entity.Aeropuerto;
import com.ecommerce.vuelos.entity.Clase;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.repository.AeropuertoRepository;
import com.ecommerce.vuelos.repository.ClaseRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Autowired
    private VueloRepository vueloRepository;

    @Autowired
    protected ClaseRepository claseRepository;

    /**
     * Los tests prueban el stock con cupos chicos (1 o 10 asientos), asi que durante
     * cada test el minimo de asientos de las clases baja a 1. Como el test es
     * @Transactional, al terminar vuelve al valor real. El minimo se prueba aparte.
     */
    @BeforeEach
    void sinMinimoDeAsientos() {
        for (Clase clase : claseRepository.findAll()) {
            clase.setMinimoAsientos(1);
            claseRepository.save(clase);
        }
    }

    @Autowired
    private AeropuertoRepository aeropuertoRepository;

    /**
     * Todas las rutas validas (ida desde Buenos Aires o vuelta a Buenos Aires) ya tienen
     * vuelos de ejemplo del seeder. Para que las busquedas de los tests encuentren solo
     * sus propios vuelos, se usan dos aeropuertos de prueba: TST (Argentina, vuelve a
     * Aeroparque) y TSX (exterior, vuelve a Ezeiza). Los crean solo los tests que los
     * usan; como el test es @Transactional, al terminar se borran.
     */
    protected void crearAeropuertosDePrueba() {
        aeropuertoRepository.save(Aeropuerto.builder().codigoIata("TST").nombre("Aeropuerto de prueba")
                .ciudad("Ciudad de prueba").provincia("Prueba").pais("Argentina")
                .zonaHoraria("America/Argentina/Buenos_Aires").build());
        aeropuertoRepository.save(Aeropuerto.builder().codigoIata("TSX").nombre("Aeropuerto de prueba exterior")
                .ciudad("Ciudad exterior").provincia("Prueba").pais("Chile")
                .zonaHoraria("America/Santiago").build());
    }

    /**
     * La API no deja crear vuelos con fecha pasada (@Future), asi que para
     * probar un vuelo que ya salio le movemos la fecha directo en la base.
     */
    protected void hacerQueYaSalio(Long vueloId) {
        Vuelo vuelo = vueloRepository.findById(vueloId).orElseThrow();
        vuelo.setFechaSalida(LocalDateTime.now().minusDays(1));
        vuelo.setFechaLlegada(LocalDateTime.now().minusDays(1).plusHours(3));
        vueloRepository.saveAndFlush(vuelo);
    }

    /** Mueve la salida del vuelo a dentro de unas horas (la API exige fechas futuras, no cercanas). */
    protected void hacerQueSalgaEn(Long vueloId, int horas) {
        Vuelo vuelo = vueloRepository.findById(vueloId).orElseThrow();
        vuelo.setFechaSalida(LocalDateTime.now().plusHours(horas));
        vuelo.setFechaLlegada(LocalDateTime.now().plusHours(horas + 3));
        vueloRepository.saveAndFlush(vuelo);
    }

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

    /**
     * La aerolinea tiene un unico vendedor: el administrador. Para los tests que
     * publican vuelos se crea un admin nuevo (lo da de alta el admin del seeder).
     */
    protected String registrarYLoguearAdmin(String username) throws Exception {
        Map<String, String> body = Map.of(
                "username", username,
                "mail", username + "@test.com",
                "password", "123456",
                "nombre", "Test",
                "apellido", "Admin"
        );
        mockMvc.perform(post("/api/auth/registro/administrador")
                        .header("Authorization", "Bearer " + loginAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
        return login(username, "123456");
    }


    protected Long categoriaPorDefecto() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/vuelos"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("content").get(0).get("categoriaId").asLong();
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
                              double precio) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("numeroVuelo", "TS" + System.nanoTime() % 100000);
        body.put("descripcion", "Vuelo de prueba");
        body.put("categoriaId", categoriaPorDefecto());
        body.put("origenIata", origenIata);
        body.put("destinoIata", destinoIata);
        body.put("fechaSalida", LocalDateTime.now().plusDays(10).withNano(0).toString());
        body.put("fechaLlegada", LocalDateTime.now().plusDays(10).plusHours(3).withNano(0).toString());
        body.put("precio", precio);

        MvcResult result = mockMvc.perform(post("/api/vuelos")
                        .header("Authorization", "Bearer " + vendedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return idDe(result);
    }


    protected Long crearDisponibilidad(String vendedorToken, Long vueloId, Long claseId,
                                       int asientos, double precio) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vueloId", vueloId);
        body.put("claseId", claseId);
        body.put("asientosTotales", asientos);
        body.put("precio", precio);

        MvcResult result = mockMvc.perform(post("/api/disponibilidades")
                        .header("Authorization", "Bearer " + vendedorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return idDe(result);
    }


    protected Long crearVueloConCupo(String vendedorToken, String origenIata, String destinoIata,
                                     double precio, int asientos) throws Exception {
        Long vueloId = crearVuelo(vendedorToken, origenIata, destinoIata, precio);
        return crearDisponibilidad(vendedorToken, vueloId, clasePorDefecto(), asientos, precio);
    }


    protected Long clasePorDefecto() throws Exception {
        return claseEnPosicion(0);
    }


    protected Long otraClase() throws Exception {
        return claseEnPosicion(1);
    }

    private Long claseEnPosicion(int indice) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/clases"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .get(indice).get("id").asLong();
    }

    protected Long agregarAlCarrito(String compradorToken, Long disponibilidadId, int cantidad) throws Exception {
        Map<String, Object> body = Map.of("disponibilidadId", disponibilidadId, "cantidad", cantidad);
        MvcResult result = mockMvc.perform(post("/api/carrito/items")
                        .header("Authorization", "Bearer " + compradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("items");
        return items.get(items.size() - 1).get("id").asLong();
    }

    private int documentoSiguiente = 20000000;

    /**
     * El checkout pide los datos de cada pasajero antes de pagar. Para los tests que no prueban eso,
     * se arma un pasajero por cada lugar del carrito, con una edad que corresponde a su tipo.
     */
    protected MockHttpServletRequestBuilder checkout(String token) throws Exception {
        return checkout(token, Map.of());
    }

    protected MockHttpServletRequestBuilder checkout(String token, Map<String, Object> extras) throws Exception {
        JsonNode carrito = objectMapper.readTree(mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString());
        List<Map<String, Object>> pasajeros = new ArrayList<>();
        JsonNode items = carrito.path("items");
        for (JsonNode item : items) {
            String tipo = item.path("tipoPasajero").asText("ADULTO");
            LocalDate nacimiento = switch (tipo) {
                case "NINO" -> LocalDate.now().minusYears(6);
                case "BEBE" -> LocalDate.now().minusMonths(6);
                default -> LocalDate.now().minusYears(30);
            };
            for (int i = 0; i < item.path("cantidad").asInt(); i++) {
                Map<String, Object> pasajero = new LinkedHashMap<>();
                pasajero.put("itemId", item.path("id").asLong());
                pasajero.put("nombre", "Pasajero");
                pasajero.put("apellido", "Prueba");
                pasajero.put("tipoDocumento", "DNI");
                pasajero.put("documento", String.valueOf(documentoSiguiente++));
                pasajero.put("fechaNacimiento", nacimiento.toString());
                pasajeros.add(pasajero);
            }
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>(extras);
        cuerpo.put("pasajeros", pasajeros);
        return json(post("/api/carrito/checkout").header("Authorization", "Bearer " + token), cuerpo);
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body) throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    protected Long idDe(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
