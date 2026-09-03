package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void registrarPasajero_devuelveDatosSinPassword() throws Exception {
        Map<String, String> body = Map.of(
                "username", "juanp",
                "mail", "juanp@test.com",
                "password", "123456",
                "nombre", "Juan",
                "apellido", "Perez"
        );

        mockMvc.perform(post("/api/auth/registro/pasajero")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("juanp"))
                .andExpect(jsonPath("$.rol").value("PASAJERO"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registrarPasajero_conUsernameDuplicado_devuelve400() throws Exception {
        Map<String, String> body = Map.of(
                "username", "duplicado",
                "mail", "duplicado1@test.com",
                "password", "123456",
                "nombre", "A",
                "apellido", "B"
        );
        mockMvc.perform(post("/api/auth/registro/pasajero")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        Map<String, String> repetido = Map.of(
                "username", "duplicado",
                "mail", "duplicado2@test.com",
                "password", "123456",
                "nombre", "A",
                "apellido", "B"
        );
        mockMvc.perform(post("/api/auth/registro/pasajero")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repetido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_conCredencialesValidas_devuelveUsuarioYCreaSesion() throws Exception {
        registrarYLoguearPasajero("loginok");
    }

    @Test
    void login_conCredencialesInvalidas_devuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "noexiste", "password", "loquesea"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegido_sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/carrito"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrarAdministrador_sinSesionDeAdmin_devuelve401() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "nuevoadmin",
                "mail", "nuevoadmin@test.com",
                "password", "123456",
                "nombre", "A",
                "apellido", "B"
        );
        mockMvc.perform(post("/api/auth/registro/administrador")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrarAdministrador_comoAdmin_devuelveCreated() throws Exception {
        String adminSession = loginAdmin();

        Map<String, Object> body = Map.of(
                "username", "nuevoadmin2",
                "mail", "nuevoadmin2@test.com",
                "password", "123456",
                "nombre", "A",
                "apellido", "B"
        );
        mockMvc.perform(post("/api/auth/registro/administrador")
                        .header("Authorization", "Bearer " + adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    void me_conSesionActiva_devuelveElUsuarioLogueado() throws Exception {
        String session = registrarYLoguearPasajero("mepasajero");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mepasajero"));
    }

    @Test
    void logout_devuelveNoContent() throws Exception {
        String token = registrarYLoguearPasajero("logoutuser");

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Con JWT stateless el logout es un no-op del lado servidor: el token
        // sigue siendo valido hasta que expira, no hay invalidacion server-side.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
