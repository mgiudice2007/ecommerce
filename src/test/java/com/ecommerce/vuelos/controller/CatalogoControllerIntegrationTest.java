package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class CatalogoControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void listarCategorias_sinToken_devuelveLasTresDelSeeder() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].nombre").value("Cabotaje"));
    }

    @Test
    void listarAeropuertos_sinToken_usaElCodigoIataComoId() throws Exception {
        mockMvc.perform(get("/api/aeropuertos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[?(@.codigoIata == 'EZE')].ciudad").value("Buenos Aires"))
                .andExpect(jsonPath("$[?(@.codigoIata == 'MAD')].pais").value("España"));
    }

    @Test
    void listarClases_sinToken_devuelveSiLlevaEquipajeEnBodega() throws Exception {
        mockMvc.perform(get("/api/clases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.nombre == 'Economica')].equipajeBodega").value(false))
                .andExpect(jsonPath("$[?(@.nombre == 'Primera')].equipajeBodega").value(true));
    }
}
