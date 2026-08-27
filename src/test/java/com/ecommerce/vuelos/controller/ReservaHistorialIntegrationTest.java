package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservaHistorialIntegrationTest extends IntegrationTestSupport {

    @Test
    void historial_devuelveSoloLasReservasDelPasajeroLogueado() throws Exception {
        MockHttpSession admin = loginAdmin();
        Long aerolineaId = crearAerolinea(admin, "Aerolinea Historial");
        Long vueloId = crearVuelo(admin, aerolineaId, "A", "B", 100.0, 10, "ECONOMICA");

        MockHttpSession pasajero1 = registrarYLoguearPasajero("pasajerohist1");
        agregarAlCarrito(pasajero1, vueloId, 1);
        mockMvc.perform(post("/api/carrito/checkout").session(pasajero1)).andExpect(status().isCreated());

        MockHttpSession pasajero2 = registrarYLoguearPasajero("pasajerohist2");

        mockMvc.perform(get("/api/reservas").session(pasajero1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/reservas").session(pasajero2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cancelarReserva_devuelveElStockAlVuelo() throws Exception {
        MockHttpSession admin = loginAdmin();
        Long aerolineaId = crearAerolinea(admin, "Aerolinea Cancelar");
        Long vueloId = crearVuelo(admin, aerolineaId, "A", "B", 100.0, 10, "ECONOMICA");
        MockHttpSession pasajero = registrarYLoguearPasajero("pasajerocancela");
        agregarAlCarrito(pasajero, vueloId, 4);

        String reservaJson = mockMvc.perform(post("/api/carrito/checkout").session(pasajero))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long reservaId = objectMapper.readTree(reservaJson).get("id").asLong();

        mockMvc.perform(post("/api/reservas/" + reservaId + "/cancelar").session(pasajero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asientosDisponibles").value(10));
    }

    @Test
    void cancelarReserva_yaCancelada_devuelve400() throws Exception {
        MockHttpSession admin = loginAdmin();
        Long aerolineaId = crearAerolinea(admin, "Aerolinea Doble Cancel");
        Long vueloId = crearVuelo(admin, aerolineaId, "A", "B", 100.0, 10, "ECONOMICA");
        MockHttpSession pasajero = registrarYLoguearPasajero("pasajerodoblecancel");
        agregarAlCarrito(pasajero, vueloId, 1);

        String reservaJson = mockMvc.perform(post("/api/carrito/checkout").session(pasajero))
                .andReturn().getResponse().getContentAsString();
        Long reservaId = objectMapper.readTree(reservaJson).get("id").asLong();

        mockMvc.perform(post("/api/reservas/" + reservaId + "/cancelar").session(pasajero))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/reservas/" + reservaId + "/cancelar").session(pasajero))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelarReserva_deOtroPasajero_devuelve404() throws Exception {
        MockHttpSession admin = loginAdmin();
        Long aerolineaId = crearAerolinea(admin, "Aerolinea Ajena");
        Long vueloId = crearVuelo(admin, aerolineaId, "A", "B", 100.0, 10, "ECONOMICA");

        MockHttpSession dueno = registrarYLoguearPasajero("pasajerodueno");
        agregarAlCarrito(dueno, vueloId, 1);
        String reservaJson = mockMvc.perform(post("/api/carrito/checkout").session(dueno))
                .andReturn().getResponse().getContentAsString();
        Long reservaId = objectMapper.readTree(reservaJson).get("id").asLong();

        MockHttpSession otro = registrarYLoguearPasajero("pasajerointruso");

        mockMvc.perform(post("/api/reservas/" + reservaId + "/cancelar").session(otro))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/reservas/" + reservaId).session(otro))
                .andExpect(status().isNotFound());
    }
}
