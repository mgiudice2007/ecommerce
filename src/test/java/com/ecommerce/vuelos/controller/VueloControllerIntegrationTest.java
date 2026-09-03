package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VueloControllerIntegrationTest extends IntegrationTestSupport {

    private Map<String, Object> bodyVuelo(Long categoriaId, String origen, String destino, double precio) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("numeroVuelo", "TS" + System.nanoTime() % 10000);
        body.put("descripcion", "Vuelo de prueba");
        body.put("categoriaId", categoriaId);
        body.put("origenIata", origen);
        body.put("destinoIata", destino);
        body.put("fechaSalida", LocalDateTime.now().plusDays(10).withNano(0).toString());
        body.put("fechaLlegada", LocalDateTime.now().plusDays(10).plusHours(3).withNano(0).toString());
        body.put("precio", precio);
        body.put("asientosDisponibles", 10);
        body.put("descuento", 0);
        body.put("clase", "ECONOMICA");
        return body;
    }

    @Test
    void buscar_sinFiltros_devuelveLosVuelosPublicados() throws Exception {
        mockMvc.perform(get("/api/vuelos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroVuelo").exists())
                .andExpect(jsonPath("$[0].vendedorUsername").exists());
    }

    @Test
    void buscar_filtrandoPorPrecioMaximo_excluyeLosCaros() throws Exception {
        String vendedor = registrarYLoguearVendedor("vfiltro");
        crearVuelo(vendedor, "COR", "MDZ", 100.0, 10, "ECONOMICA");
        crearVuelo(vendedor, "COR", "MDZ", 900.0, 10, "PRIMERA");

        mockMvc.perform(get("/api/vuelos")
                        .param("origen", "COR")
                        .param("precioMax", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].precio").value(100.0));
    }

    @Test
    void buscar_filtrandoPorCategoria_soloDevuelveEsaCategoria() throws Exception {
        Long categoria = categoriaPorDefecto();
        mockMvc.perform(get("/api/vuelos").param("categoriaId", categoria.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoriaId").value(categoria));
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
                .andExpect(jsonPath("$.duracionMinutos").value(180));
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
    void modificarVueloDeOtroVendedor_devuelve400() throws Exception {
        String dueno = registrarYLoguearVendedor("vpropietario");
        Long vueloId = crearVuelo(dueno, "EZE", "MAD", 300.0, 5, "ECONOMICA");

        String intruso = registrarYLoguearVendedor("vintruso");
        mockMvc.perform(put("/api/vuelos/" + vueloId)
                        .header("Authorization", "Bearer " + intruso)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bodyVuelo(categoriaPorDefecto(), "EZE", "MAD", 999.0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarVuelo_esBajaLogicaYDejaDeListarse() throws Exception {
        String vendedor = registrarYLoguearVendedor("vbaja");
        Long vueloId = crearVuelo(vendedor, "MDZ", "COR", 250.0, 5, "ECONOMICA");

        mockMvc.perform(delete("/api/vuelos/" + vueloId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isNoContent());

        // sigue existiendo por id, pero con estado ELIMINADO
        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ELIMINADO"));

        // y ya no aparece en el listado
        mockMvc.perform(get("/api/vuelos").param("origen", "MDZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
