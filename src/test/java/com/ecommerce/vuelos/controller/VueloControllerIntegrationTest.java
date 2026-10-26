package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import com.ecommerce.vuelos.entity.Clase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VueloControllerIntegrationTest extends IntegrationTestSupport {

    @BeforeEach
    void rutasDePrueba() {
        crearAeropuertosDePrueba();
    }

    private Map<String, Object> bodyVuelo(Long categoriaId, String origen, String destino, double precio)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("numeroVuelo", "TS" + System.nanoTime() % 100000);
        body.put("descripcion", "Vuelo de prueba");
        body.put("categoriaId", categoriaId);
        body.put("origenIata", origen);
        body.put("destinoIata", destino);
        body.put("fechaSalida", LocalDateTime.now().plusDays(10).withNano(0).toString());
        body.put("fechaLlegada", LocalDateTime.now().plusDays(10).plusHours(3).withNano(0).toString());
        body.put("precio", precio);
        return body;
    }

    @Test
    void buscar_sinFiltros_devuelveLosVuelosPublicados() throws Exception {
        mockMvc.perform(get("/api/vuelos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numeroVuelo").exists())
                .andExpect(jsonPath("$.content[0].vendedorUsername").exists())
                .andExpect(jsonPath("$.content[0].disponibilidades").isArray());
    }

    @Test
    void buscar_noListaLosVuelosQueYaSalieron_peroSiguenRespondiendoPorId() throws Exception {
        String vendedor = registrarYLoguearAdmin("vsalio");
        Long vueloId = crearVuelo(vendedor, "TST", "AEP", 500.0);
        hacerQueYaSalio(vueloId);

        mockMvc.perform(get("/api/vuelos").param("origen", "TST").param("destino", "AEP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + vueloId + ")]").isEmpty());

        // Las compras viejas lo siguen necesitando para mostrar sus datos
        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk());
    }

    @Test
    void buscar_filtrandoPorFecha_soloDevuelveLosQueSalenEsosDias() throws Exception {
        String vendedor = registrarYLoguearAdmin("vfecha");
        Long vueloId = crearVuelo(vendedor, "TST", "AEP", 300.0); // sale dentro de 10 dias
        String diaDelVuelo = LocalDate.now().plusDays(10).toString();
        String diaSiguiente = LocalDate.now().plusDays(11).toString();

        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "TST")
                        .param("fechaDesde", diaDelVuelo)
                        .param("fechaHasta", diaDelVuelo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + vueloId + ")]").isNotEmpty());

        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "TST")
                        .param("fechaDesde", diaSiguiente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + vueloId + ")]").isEmpty());
    }

    @Test
    void buscar_filtrandoPorPrecioMaximo_excluyeLosCaros() throws Exception {
        String vendedor = registrarYLoguearAdmin("vfiltro");
        crearVuelo(vendedor, "TST", "AEP", 100.0);
        crearVuelo(vendedor, "TST", "AEP", 900.0);

        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "TST")
                        .param("precioMax", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].precio").value(100.0));
    }

    @Test
    void buscar_conPageYSize_devuelveSoloEsaPagina() throws Exception {
        String vendedor = registrarYLoguearAdmin("vpagina");
        crearVuelo(vendedor, "TSX", "EZE", 100.0);
        crearVuelo(vendedor, "TSX", "EZE", 200.0);
        crearVuelo(vendedor, "TSX", "EZE", 300.0);

        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "TSX")
                        .param("destino", "EZE")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0));


        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "TSX")
                        .param("destino", "EZE")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.last").value(true));


        mockMvc.perform(get("/api/vuelos").param("origen", "TSX").param("destino", "EZE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void buscar_filtrandoPorClase_soloDevuelveVuelosConEseCupo() throws Exception {
        String vendedor = registrarYLoguearAdmin("vclase");
        Long conCupo = crearVuelo(vendedor, "TST", "AEP", 400.0);
        crearDisponibilidad(vendedor, conCupo, clasePorDefecto(), 10, 400.0);
        crearVuelo(vendedor, "TST", "AEP", 400.0); // sin cupos cargados

        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "TST")
                        .param("destino", "AEP")
                        .param("claseId", clasePorDefecto().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(conCupo));
    }

    @Test
    void crearVuelo_comoVendedor_quedaComoDueno() throws Exception {
        String vendedor = registrarYLoguearAdmin("vdueno");

        mockMvc.perform(post("/api/vuelos")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bodyVuelo(categoriaPorDefecto(), "EZE", "MIA", 500.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendedorUsername").value("vdueno"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.duracionMinutos").value(180))
                .andExpect(jsonPath("$.hayStock").value(false));
    }

    @Test
    void crearVuelo_comoComprador_devuelve403() throws Exception {
        String comprador = registrarYLoguearComprador("cnopublica");

        mockMvc.perform(post("/api/vuelos")
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bodyVuelo(categoriaPorDefecto(), "EZE", "MIA", 500.0))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cargarCupo_quedaConStockYPrecioConDescuento() throws Exception {
        String vendedor = registrarYLoguearAdmin("vcupo");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 30, 1000.0);

        mockMvc.perform(get("/api/disponibilidades/" + cupoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asientosTotales").value(30))
                .andExpect(jsonPath("$.asientosDisponibles").value(30))
                .andExpect(jsonPath("$.hayStock").value(true));

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayStock").value(true))
                .andExpect(jsonPath("$.disponibilidades.length()").value(1));
    }

    @Test
    void listarCuposDelVuelo_devuelveUnoPorClaseCargada() throws Exception {
        String vendedor = registrarYLoguearAdmin("vcupolista");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);
        crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 30, 1000.0);
        crearDisponibilidad(vendedor, vueloId, otraClase(), 10, 2500.0);


        mockMvc.perform(get("/api/disponibilidades").param("vueloId", vueloId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].vueloId").value(vueloId))
                .andExpect(jsonPath("$[0].claseNombre").exists())
                .andExpect(jsonPath("$[0].hayStock").value(true));


        Long sinCupos = crearVuelo(vendedor, "EZE", "MAD", 500.0);
        mockMvc.perform(get("/api/disponibilidades").param("vueloId", sinCupos.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cargarDosVecesLaMismaClase_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vduplicado");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 800.0);
        crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 800.0);

        Map<String, Object> repetido = Map.of(
                "vueloId", vueloId,
                "claseId", clasePorDefecto(),
                "asientosTotales", 5,
                "precio", 800.0);

        mockMvc.perform(post("/api/disponibilidades")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repetido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cargarMasAsientosQueElMaximoDeLaClase_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vmaximo");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 800.0);

        Map<String, Object> demasiados = Map.of(
                "vueloId", vueloId,
                "claseId", clasePorDefecto(),
                "asientosTotales", 8988986,
                "precio", 800.0);

        mockMvc.perform(post("/api/disponibilidades")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demasiados)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cargarMenosAsientosQueElMinimoDeLaClase_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vminimo");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 800.0);
        Clase economica = claseRepository.findById(clasePorDefecto()).orElseThrow();
        economica.setMinimoAsientos(50); // el valor real (en los tests baja a 1)

        Map<String, Object> pocos = Map.of(
                "vueloId", vueloId,
                "claseId", economica.getId(),
                "asientosTotales", 1,
                "precio", 800.0);

        mockMvc.perform(post("/api/disponibilidades")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pocos)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void claseMejorMasBarataQueUnaBasica_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vprecios");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);
        crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 30, 1000.0);

        // Ejecutiva a 500 cuando Economica cuesta 1000
        Map<String, Object> ejecutivaBarata = Map.of(
                "vueloId", vueloId,
                "claseId", otraClase(),
                "asientosTotales", 10,
                "precio", 500.0);

        mockMvc.perform(post("/api/disponibilidades")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ejecutivaBarata)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarClaseSinCompras_laSaca() throws Exception {
        String vendedor = registrarYLoguearAdmin("vborrar");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 1000.0);

        // Estaba en el carrito de alguien: se saca de ese carrito
        String comprador = registrarYLoguearComprador("cborrar");
        agregarAlCarrito(comprador, cupoId, 1);

        mockMvc.perform(delete("/api/disponibilidades/" + cupoId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/disponibilidades/" + cupoId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void eliminarClaseConCompras_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vnoborrar");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 1000.0);

        String comprador = registrarYLoguearComprador("cnoborrar");
        agregarAlCarrito(comprador, cupoId, 1);
        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/disponibilidades/" + cupoId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vueloQueNoPasaPorBuenosAires_oPorElAeropuertoEquivocado_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vrutas");
        for (String[] ruta : new String[][]{{"COR", "MDZ"}, {"AEP", "MAD"}, {"EZE", "COR"}, {"AEP", "EZE"}}) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("numeroVuelo", "RUTA" + ruta[0] + ruta[1]);
            body.put("descripcion", "Ruta invalida");
            body.put("categoriaId", categoriaPorDefecto());
            body.put("origenIata", ruta[0]);
            body.put("destinoIata", ruta[1]);
            body.put("fechaSalida", LocalDateTime.now().plusDays(5).withNano(0).toString());
            body.put("fechaLlegada", LocalDateTime.now().plusDays(5).plusHours(2).withNano(0).toString());

            mockMvc.perform(post("/api/vuelos")
                            .header("Authorization", "Bearer " + vendedor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void buscarPorTexto_encuentraPorCiudadONumeroDeVuelo() throws Exception {
        String vendedor = registrarYLoguearAdmin("vtexto");
        crearVuelo(vendedor, "TST", "AEP", 300.0);

        mockMvc.perform(get("/api/vuelos").param("texto", "ciudad de prueba").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/vuelos").param("texto", "no existe esta ciudad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void resumenDelPanel_cuentaLosVuelosYLosAsientos() throws Exception {
        String vendedor = registrarYLoguearAdmin("vresumen");
        String antes = mockMvc.perform(get("/api/vuelos/resumen"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long vuelosAntes = objectMapper.readTree(antes).get("vuelos").asLong();
        long asientosAntes = objectMapper.readTree(antes).get("asientosTotales").asLong();

        crearVueloConCupo(vendedor, "TST", "AEP", 300.0, 10);

        mockMvc.perform(get("/api/vuelos/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vuelos").value(vuelosAntes + 1))
                .andExpect(jsonPath("$.asientosTotales").value(asientosAntes + 10));
    }

    @Test
    void vueloQueDuraMasDe20Horas_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vlargo");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("numeroVuelo", "LARGO1");
        body.put("descripcion", "Vuelo imposible");
        body.put("categoriaId", categoriaPorDefecto());
        body.put("origenIata", "AEP");
        body.put("destinoIata", "COR");
        body.put("fechaSalida", LocalDateTime.now().plusDays(5).withNano(0).toString());
        body.put("fechaLlegada", LocalDateTime.now().plusDays(12).withNano(0).toString()); // 7 dias

        mockMvc.perform(post("/api/vuelos")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elPrecioDelVuelo_esElDeSuClaseMasBarata() throws Exception {
        String vendedor = registrarYLoguearAdmin("vpreciosolo");
        // Se publica sin precio: lo ponen las clases
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("numeroVuelo", "SINPRECIO1");
        body.put("descripcion", "Vuelo de prueba");
        body.put("categoriaId", categoriaPorDefecto());
        body.put("origenIata", "EZE");
        body.put("destinoIata", "MAD");
        body.put("fechaSalida", LocalDateTime.now().plusDays(10).withNano(0).toString());
        body.put("fechaLlegada", LocalDateTime.now().plusDays(10).plusHours(12).withNano(0).toString());
        Long vueloId = idDe(mockMvc.perform(post("/api/vuelos")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn());

        crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 30, 900.0);
        crearDisponibilidad(vendedor, vueloId, otraClase(), 10, 2500.0);

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(900.0));
    }

    @Test
    void cargarCeroAsientos_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vcero");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 800.0);

        Map<String, Object> cero = Map.of(
                "vueloId", vueloId,
                "claseId", clasePorDefecto(),
                "asientosTotales", 0,
                "precio", 800.0);

        mockMvc.perform(post("/api/disponibilidades")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cero)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modificarVuelo_comoPasajero_devuelve403() throws Exception {
        // Hay un unico vendedor (el admin): un pasajero no puede tocar vuelos
        String admin = registrarYLoguearAdmin("vpropietario");
        Long vueloId = crearVuelo(admin, "EZE", "MAD", 300.0);

        String intruso = registrarYLoguearComprador("vintruso");
        mockMvc.perform(put("/api/vuelos/" + vueloId)
                        .header("Authorization", "Bearer " + intruso)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bodyVuelo(categoriaPorDefecto(), "EZE", "MAD", 999.0))))
                .andExpect(status().isForbidden());
    }

    private void cambiarEstado(String token, Long vueloId, String estado, int statusEsperado) throws Exception {
        mockMvc.perform(patch("/api/vuelos/" + vueloId + "/estado")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"" + estado + "\"}"))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    void eliminarVuelo_esBajaLogicaPorPatchYDejaDeListarse() throws Exception {
        String vendedor = registrarYLoguearAdmin("vbaja");
        Long vueloId = crearVuelo(vendedor, "TST", "AEP", 250.0);

        mockMvc.perform(patch("/api/vuelos/" + vueloId + "/estado")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ELIMINADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ELIMINADO"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ELIMINADO"));

        mockMvc.perform(get("/api/vuelos").param("origen", "TST").param("destino", "AEP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void eliminarVuelo_yaNoExisteElDelete() throws Exception {
        String vendedor = registrarYLoguearAdmin("vsindelete");
        Long vueloId = crearVuelo(vendedor, "TST", "AEP", 250.0);

        mockMvc.perform(delete("/api/vuelos/" + vueloId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void marcarVueloDemorado_loDejaListadoYNoTocaLaDescripcion() throws Exception {
        String vendedor = registrarYLoguearAdmin("vdemora");
        Long vueloId = crearVuelo(vendedor, "TST", "AEP", 250.0);

        mockMvc.perform(patch("/api/vuelos/" + vueloId + "/estado")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"DEMORADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DEMORADO"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        // la descripcion no se toca: el estado es un atributo propio del vuelo
        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(jsonPath("$.estado").value("DEMORADO"))
                .andExpect(jsonPath("$.descripcion").value("Vuelo de prueba"));

        mockMvc.perform(get("/api/vuelos").param("origen", "TST").param("destino", "AEP"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void vueloDemorado_sePuedeComprar_peroPausadoNo() throws Exception {
        String vendedor = registrarYLoguearAdmin("vcompra");
        Long vueloId = crearVuelo(vendedor, "TST", "AEP", 250.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 250.0);
        String comprador = registrarYLoguearComprador("compradorestado");

        cambiarEstado(vendedor, vueloId, "DEMORADO", 200);
        agregarAlCarrito(comprador, cupoId, 1);

        cambiarEstado(vendedor, vueloId, "PAUSADO", 200);
        mockMvc.perform(post("/api/carrito/items")
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponibilidadId\":" + cupoId + ",\"cantidad\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarEstado_comoPasajero_devuelve403() throws Exception {
        String admin = registrarYLoguearAdmin("vduenoestado");
        Long vueloId = crearVuelo(admin, "TST", "AEP", 250.0);
        String intruso = registrarYLoguearComprador("vintrusoestado");

        cambiarEstado(intruso, vueloId, "CANCELADO", 403);
    }

    @Test
    void cambiarEstado_aUnoInvalidoORepetido_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("vestadoinv");
        Long vueloId = crearVuelo(vendedor, "TST", "AEP", 250.0);

        cambiarEstado(vendedor, vueloId, "VOLANDO", 400);
        cambiarEstado(vendedor, vueloId, "ACTIVO", 400);
        mockMvc.perform(patch("/api/vuelos/" + vueloId + "/estado")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vueloEliminado_noSePuedeVolverAModificar() throws Exception {
        String vendedor = registrarYLoguearAdmin("vreviv");
        Long vueloId = crearVuelo(vendedor, "TST", "AEP", 250.0);

        cambiarEstado(vendedor, vueloId, "ELIMINADO", 200);
        cambiarEstado(vendedor, vueloId, "ACTIVO", 400);
    }

    @Test
    void cambiarEstado_sinToken_devuelve401() throws Exception {
        mockMvc.perform(patch("/api/vuelos/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"DEMORADO\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void busquedaSinResultados_devuelveMensaje() throws Exception {
        mockMvc.perform(get("/api/vuelos").param("origen", "ZZZZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.mensaje").value("No hay vuelos que coincidan con los filtros indicados"));
    }

    @Test
    void busquedaConResultados_noTraeMensaje() throws Exception {
        String vendedor = registrarYLoguearAdmin("vconmensaje");
        crearVuelo(vendedor, "TST", "AEP", 250.0);

        mockMvc.perform(get("/api/vuelos").param("origen", "TST").param("destino", "AEP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.mensaje").doesNotExist());
    }
}
