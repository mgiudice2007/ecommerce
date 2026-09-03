package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrdenHistorialIntegrationTest extends IntegrationTestSupport {

    @Test
    void historial_devuelveSoloLasOrdenesDelPasajeroLogueado() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 100.0, 10);

        String pasajero1 = registrarYLoguearComprador("pasajerohist1");
        agregarAlCarrito(pasajero1, cupoId, 1);
        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + pasajero1)).andExpect(status().isCreated());

        String pasajero2 = registrarYLoguearComprador("pasajerohist2");

        mockMvc.perform(get("/api/ordenes").header("Authorization", "Bearer " + pasajero1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/ordenes").header("Authorization", "Bearer " + pasajero2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cancelarOrden_devuelveElStockAlVuelo() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 100.0, 10);
        String pasajero = registrarYLoguearComprador("pasajerocancela");
        agregarAlCarrito(pasajero, cupoId, 4);

        String ordenJson = mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long ordenId = objectMapper.readTree(ordenJson).get("id").asLong();

        mockMvc.perform(post("/api/ordenes/" + ordenId + "/cancelar").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mockMvc.perform(get("/api/disponibilidades/" + cupoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asientosDisponibles").value(10));
    }

    @Test
    void cancelarOrden_yaCancelada_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 100.0, 10);
        String pasajero = registrarYLoguearComprador("pasajerodoblecancel");
        agregarAlCarrito(pasajero, cupoId, 1);

        String ordenJson = mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + pasajero))
                .andReturn().getResponse().getContentAsString();
        Long ordenId = objectMapper.readTree(ordenJson).get("id").asLong();

        mockMvc.perform(post("/api/ordenes/" + ordenId + "/cancelar").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/ordenes/" + ordenId + "/cancelar").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelarOrden_deOtroPasajero_devuelve404() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 100.0, 10);

        String dueno = registrarYLoguearComprador("pasajerodueno");
        agregarAlCarrito(dueno, cupoId, 1);
        String ordenJson = mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + dueno))
                .andReturn().getResponse().getContentAsString();
        Long ordenId = objectMapper.readTree(ordenJson).get("id").asLong();

        String otro = registrarYLoguearComprador("pasajerointruso");

        mockMvc.perform(post("/api/ordenes/" + ordenId + "/cancelar").header("Authorization", "Bearer " + otro))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/ordenes/" + ordenId).header("Authorization", "Bearer " + otro))
                .andExpect(status().isNotFound());
    }
}
