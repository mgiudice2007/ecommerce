package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AerolineaControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void listarAerolineas_esPublico() throws Exception {
        mockMvc.perform(get("/api/aerolineas"))
                .andExpect(status().isOk());
    }

    @Test
    void crearAerolinea_comoAdmin_devuelveCreated() throws Exception {
        MockHttpSession admin = loginAdmin();

        mockMvc.perform(json(post("/api/aerolineas").session(admin), Map.of("nombre", "LATAM Test")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("LATAM Test"))
                .andExpect(jsonPath("$.cantidadVuelos").value(0));
    }

    @Test
    void crearAerolinea_sinAutenticar_devuelve401() throws Exception {
        mockMvc.perform(json(post("/api/aerolineas"), Map.of("nombre", "Sin Auth")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearAerolinea_conNombreDuplicado_devuelve400() throws Exception {
        MockHttpSession admin = loginAdmin();
        crearAerolinea(admin, "Duplicada");

        mockMvc.perform(json(post("/api/aerolineas").session(admin), Map.of("nombre", "Duplicada")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarAerolineaConVuelosAsociados_devuelve400() throws Exception {
        MockHttpSession admin = loginAdmin();
        Long aerolineaId = crearAerolinea(admin, "Con Vuelos");
        crearVuelo(admin, aerolineaId, "A", "B", 100.0, 5, "ECONOMICA");

        mockMvc.perform(delete("/api/aerolineas/" + aerolineaId).session(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarAerolinea_comoAdmin() throws Exception {
        MockHttpSession admin = loginAdmin();
        Long aerolineaId = crearAerolinea(admin, "Nombre Viejo");

        mockMvc.perform(json(put("/api/aerolineas/" + aerolineaId).session(admin), Map.of("nombre", "Nombre Nuevo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre Nuevo"));
    }
}
