package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VueloControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void listarVuelos_esPublico_sinNecesidadDeSesion() throws Exception {
        mockMvc.perform(get("/api/vuelos"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarVuelos_filtraPorClaseYRangoDePrecio() throws Exception {
        String admin = loginAdmin();
        Long aerolineaId = crearAerolinea(admin, "Aerolinea Filtros");
        crearVuelo(admin, aerolineaId, "Rosario", "Salta", 100.0, 10, "ECONOMICA");
        crearVuelo(admin, aerolineaId, "Rosario", "Ushuaia", 900.0, 10, "PRIMERA");

        mockMvc.perform(get("/api/vuelos")
                        .param("clase", "PRIMERA")
                        .param("precioMin", "500")
                        .param("precioMax", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.destino == 'Ushuaia')]").exists())
                .andExpect(jsonPath("$[?(@.destino == 'Salta')]").doesNotExist());
    }

    @Test
    void crearVuelo_comoAdmin_devuelveCreated() throws Exception {
        String admin = loginAdmin();
        Long aerolineaId = crearAerolinea(admin, "Aerolinea Crear Vuelo");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("origen", "Mendoza");
        body.put("destino", "Neuquen");
        body.put("fechaSalida", LocalDateTime.now().plusDays(5).withNano(0).toString());
        body.put("precio", 150.0);
        body.put("asientosDisponibles", 20);
        body.put("descuento", 5);
        body.put("clase", "ECONOMICA");
        body.put("aerolineaId", aerolineaId);

        mockMvc.perform(json(post("/api/vuelos").header("Authorization", "Bearer " + admin), body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioConDescuento").value(142.5));
    }

    @Test
    void crearVuelo_comoPasajero_devuelve403() throws Exception {
        String admin = loginAdmin();
        Long aerolineaId = crearAerolinea(admin, "Aerolinea Sin Permiso");
        String pasajero = registrarYLoguearComprador("pasajerovuelo");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("origen", "A");
        body.put("destino", "B");
        body.put("fechaSalida", LocalDateTime.now().plusDays(5).withNano(0).toString());
        body.put("precio", 100.0);
        body.put("asientosDisponibles", 10);
        body.put("clase", "ECONOMICA");
        body.put("aerolineaId", aerolineaId);

        mockMvc.perform(json(post("/api/vuelos").header("Authorization", "Bearer " + pasajero), body))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarYEliminarVuelo_comoAdmin() throws Exception {
        String admin = loginAdmin();
        Long aerolineaId = crearAerolinea(admin, "Aerolinea Update Delete");
        Long vueloId = crearVuelo(admin, aerolineaId, "X", "Y", 200.0, 5, "ECONOMICA");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("origen", "X");
        body.put("destino", "Z");
        body.put("fechaSalida", LocalDateTime.now().plusDays(5).withNano(0).toString());
        body.put("precio", 250.0);
        body.put("asientosDisponibles", 8);
        body.put("clase", "EJECUTIVA");
        body.put("aerolineaId", aerolineaId);

        mockMvc.perform(json(put("/api/vuelos/" + vueloId).header("Authorization", "Bearer " + admin), body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destino").value("Z"))
                .andExpect(jsonPath("$.clase").value("EJECUTIVA"));

        mockMvc.perform(delete("/api/vuelos/" + vueloId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isNotFound());
    }
}
