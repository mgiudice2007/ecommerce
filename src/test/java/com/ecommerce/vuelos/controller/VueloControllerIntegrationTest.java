package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

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
        String vendedor = registrarYLoguearVendedor("vsalio");
        Long vueloId = crearVuelo(vendedor, "COR", "MIA", 500.0);
        hacerQueYaSalio(vueloId);

        mockMvc.perform(get("/api/vuelos").param("origen", "COR").param("destino", "MIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + vueloId + ")]").isEmpty());

        // Las compras viejas lo siguen necesitando para mostrar sus datos
        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk());
    }

    @Test
    void buscar_filtrandoPorPrecioMaximo_excluyeLosCaros() throws Exception {
        String vendedor = registrarYLoguearVendedor("vfiltro");
        crearVuelo(vendedor, "COR", "MDZ", 100.0);
        crearVuelo(vendedor, "COR", "MDZ", 900.0);

        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "COR")
                        .param("precioMax", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].precio").value(100.0));
    }

    @Test
    void buscar_conPageYSize_devuelveSoloEsaPagina() throws Exception {
        String vendedor = registrarYLoguearVendedor("vpagina");
        crearVuelo(vendedor, "MIA", "COR", 100.0);
        crearVuelo(vendedor, "MIA", "COR", 200.0);
        crearVuelo(vendedor, "MIA", "COR", 300.0);

        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "MIA")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0));


        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "MIA")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.last").value(true));


        mockMvc.perform(get("/api/vuelos").param("origen", "MIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void buscar_filtrandoPorClase_soloDevuelveVuelosConEseCupo() throws Exception {
        String vendedor = registrarYLoguearVendedor("vclase");
        Long conCupo = crearVuelo(vendedor, "MDZ", "MIA", 400.0);
        crearDisponibilidad(vendedor, conCupo, clasePorDefecto(), 10, 400.0);
        crearVuelo(vendedor, "MDZ", "MIA", 400.0); // sin cupos cargados

        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "MDZ")
                        .param("claseId", clasePorDefecto().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(conCupo));
    }

    @Test
    void crearVuelo_comoVendedor_quedaComoDueno() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdueno");

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
        String vendedor = registrarYLoguearVendedor("vcupo");
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
        String vendedor = registrarYLoguearVendedor("vcupolista");
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
        String vendedor = registrarYLoguearVendedor("vduplicado");
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
    void modificarVueloDeOtroVendedor_devuelve400() throws Exception {
        String dueno = registrarYLoguearVendedor("vpropietario");
        Long vueloId = crearVuelo(dueno, "EZE", "MAD", 300.0);

        String intruso = registrarYLoguearVendedor("vintruso");
        mockMvc.perform(put("/api/vuelos/" + vueloId)
                        .header("Authorization", "Bearer " + intruso)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bodyVuelo(categoriaPorDefecto(), "EZE", "MAD", 999.0))))
                .andExpect(status().isBadRequest());
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
        String vendedor = registrarYLoguearVendedor("vbaja");
        Long vueloId = crearVuelo(vendedor, "MDZ", "COR", 250.0);

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

        mockMvc.perform(get("/api/vuelos").param("origen", "MDZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void eliminarVuelo_yaNoExisteElDelete() throws Exception {
        String vendedor = registrarYLoguearVendedor("vsindelete");
        Long vueloId = crearVuelo(vendedor, "MDZ", "COR", 250.0);

        mockMvc.perform(delete("/api/vuelos/" + vueloId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void marcarVueloDemorado_loDejaListadoYNoTocaLaDescripcion() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdemora");
        Long vueloId = crearVuelo(vendedor, "MDZ", "COR", 250.0);

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

        mockMvc.perform(get("/api/vuelos").param("origen", "MDZ"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void vueloDemorado_sePuedeComprar_peroPausadoNo() throws Exception {
        String vendedor = registrarYLoguearVendedor("vcompra");
        Long vueloId = crearVuelo(vendedor, "MDZ", "COR", 250.0);
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
    void cambiarEstadoDeVueloAjeno_devuelve400() throws Exception {
        String dueno = registrarYLoguearVendedor("vduenoestado");
        Long vueloId = crearVuelo(dueno, "MDZ", "COR", 250.0);
        String intruso = registrarYLoguearVendedor("vintrusoestado");

        cambiarEstado(intruso, vueloId, "CANCELADO", 400);
    }

    @Test
    void cambiarEstado_aUnoInvalidoORepetido_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("vestadoinv");
        Long vueloId = crearVuelo(vendedor, "MDZ", "COR", 250.0);

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
        String vendedor = registrarYLoguearVendedor("vreviv");
        Long vueloId = crearVuelo(vendedor, "MDZ", "COR", 250.0);

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

}
