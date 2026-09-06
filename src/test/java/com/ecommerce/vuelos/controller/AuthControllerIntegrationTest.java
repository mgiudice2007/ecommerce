package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void registrarComprador_devuelveDatosSinPassword() throws Exception {
        Map<String, String> body = Map.of(
                "username", "juanp",
                "mail", "juanp@test.com",
                "password", "123456",
                "nombre", "Juan",
                "apellido", "Perez",
                "rol", "COMPRADOR"
        );

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("juanp"))
                .andExpect(jsonPath("$.rol").value("COMPRADOR"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registro_noExigeDniNiFechaDeNacimiento() throws Exception {
        String token = registrarYLoguearComprador("csindni");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").doesNotExist())
                .andExpect(jsonPath("$.fechaNacimiento").doesNotExist())
                .andExpect(jsonPath("$.fechaRegistro").exists());
    }

    @Test
    void actualizarPerfil_completaLosDatosDePasajero() throws Exception {
        String token = registrarYLoguearComprador("cperfil");

        Map<String, String> perfil = Map.of(
                "nombre", "Ana Laura",
                "apellido", "Perez",
                "dni", "30111222",
                "fechaNacimiento", "1995-06-15",
                "telefono", "1155667788"
        );

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Laura"))
                .andExpect(jsonPath("$.dni").value("30111222"))
                .andExpect(jsonPath("$.fechaNacimiento").value("1995-06-15"))
                .andExpect(jsonPath("$.password").doesNotExist());


        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.dni").value("30111222"));
    }

    @Test
    void actualizarPerfil_conDniDeOtraCuenta_devuelve400() throws Exception {
        String primero = registrarYLoguearComprador("cdniuno");
        Map<String, String> perfil = Map.of(
                "nombre", "Uno", "apellido", "Uno",
                "dni", "28999888", "fechaNacimiento", "1990-01-01");

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + primero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(perfil)))
                .andExpect(status().isOk());

        String segundo = registrarYLoguearComprador("cdnidos");
        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + segundo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(perfil)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarPerfil_conDniInvalido_devuelve400() throws Exception {
        String token = registrarYLoguearComprador("cdnimal");
        Map<String, String> perfil = Map.of(
                "nombre", "Ana", "apellido", "Perez", "dni", "ABC123");

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(perfil)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elPerfilSinToken_devuelve401_yNo500() throws Exception {
        // /api/auth/** estaba entero en permitAll, asi que /me llegaba al
        // controller con el principal en null y explotaba con 500.
        Map<String, String> perfil = Map.of("nombre", "Ana", "apellido", "Perez");

        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(perfil)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());


        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "admin123"))))
                .andExpect(status().isOk());
    }

    @Test
    void registrarComprador_conUsernameDuplicado_devuelve400() throws Exception {
        Map<String, String> body = Map.of(
                "username", "duplicado",
                "mail", "duplicado1@test.com",
                "password", "123456",
                "nombre", "A",
                "apellido", "B",
                "rol", "COMPRADOR"
        );
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        Map<String, String> repetido = Map.of(
                "username", "duplicado",
                "mail", "duplicado2@test.com",
                "password", "123456",
                "nombre", "A",
                "apellido", "B",
                "rol", "COMPRADOR"
        );
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repetido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_conCredencialesValidas_devuelveUsuarioYCreaSesion() throws Exception {
        registrarYLoguearComprador("loginok");
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
                "apellido", "B",
                "rol", "ADMIN"
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
                "apellido", "B",
                "rol", "ADMIN"
        );
        mockMvc.perform(post("/api/auth/registro/administrador")
                        .header("Authorization", "Bearer " + adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void me_conSesionActiva_devuelveElUsuarioLogueado() throws Exception {
        String session = registrarYLoguearComprador("mepasajero");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mepasajero"));
    }

    @Test
    void logout_devuelveNoContent() throws Exception {
        String token = registrarYLoguearComprador("logoutuser");

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
