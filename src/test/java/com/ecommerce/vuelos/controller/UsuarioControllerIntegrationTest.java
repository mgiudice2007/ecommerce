package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerIntegrationTest extends IntegrationTestSupport {

    /** Busca el id de un usuario en el listado del admin. */
    private Long idDeUsuario(String adminToken, String username) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        for (var nodo : objectMapper.readTree(r.getResponse().getContentAsString())) {
            if (nodo.get("username").asText().equals(username)) {
                return nodo.get("id").asLong();
            }
        }
        throw new AssertionError("No se encontro el usuario " + username);
    }

    @Test
    void listarUsuarios_comoAdmin_devuelveTodosSinDatosDePasajero() throws Exception {
        registrarYLoguearComprador("listado1");
        String admin = loginAdmin();

        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'listado1')].rol").value("COMPRADOR"))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].dni").doesNotExist());
    }

    @Test
    void listarUsuarios_comoComprador_devuelve403() throws Exception {
        String comprador = registrarYLoguearComprador("curioso");

        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarRol_deCompradorAAdmin_leDaPermisosEnseguida() throws Exception {
        String comprador = registrarYLoguearComprador("ascendido");
        String admin = loginAdmin();
        Long id = idDeUsuario(admin, "ascendido");

        mockMvc.perform(put("/api/usuarios/" + id + "/rol")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rol", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"));

        // Con el mismo token de antes ya puede publicar vuelos (solo el admin puede): el rol se lee de la base
        crearVuelo(comprador, "AEP", "COR", 400.0);

        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarRol_deVendedorAComprador_puedeUsarElCarrito() throws Exception {
        String vendedor = registrarYLoguearAdmin("degradado");
        String admin = loginAdmin();
        Long id = idDeUsuario(admin, "degradado");

        mockMvc.perform(put("/api/usuarios/" + id + "/rol")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rol", "COMPRADOR"))))
                .andExpect(status().isOk());

        // Los vendedores no nacen con carrito: se le crea al usarlo por primera vez
        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void cambiarRol_propio_devuelve400() throws Exception {
        String admin = loginAdmin();
        Long id = idDeUsuario(admin, "admin");

        mockMvc.perform(put("/api/usuarios/" + id + "/rol")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rol", "COMPRADOR"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarRol_comoComprador_devuelve403() throws Exception {
        String comprador = registrarYLoguearComprador("compradorvivo");

        mockMvc.perform(put("/api/usuarios/1/rol")
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rol", "ADMIN"))))
                .andExpect(status().isForbidden());
    }
}
