package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las reglas de acceso por rol viven en SecurityConfig. Estos tests recorren la
 * matriz de rutas contra roles y los distintos tokens que un cliente puede mandar.
 */
class SecurityJwtIntegrationTest extends IntegrationTestSupport {

    @Value("${jwt.secret}")
    private String secret;

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String tokenConVencimiento(long vencimientoMs) {
        return Jwts.builder()
                .subject("admin")
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(vencimientoMs))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .compact();
    }

    @Test
    void sinToken_enRutaProtegida_devuelve401ConJson() throws Exception {
        mockMvc.perform(post("/api/vuelos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("No autenticado"));
    }

    @Test
    void tokenInventado_devuelve401ConMotivo() throws Exception {
        mockMvc.perform(get("/api/carrito").header("Authorization", bearer("esto.no.es.un.jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token invalido"));
    }

    @Test
    void tokenAdulterado_devuelve401() throws Exception {
        String token = registrarYLoguearComprador("tokenadulterado");
        // cambiar el ultimo caracter de la firma invalida el token
        String adulterado = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/api/carrito").header("Authorization", bearer(adulterado)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token invalido"));
    }

    @Test
    void tokenVencido_devuelve401ConMotivo() throws Exception {
        String vencido = tokenConVencimiento(System.currentTimeMillis() - 3_600_000);

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(vencido)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token vencido"));
    }

    @Test
    void tokenFirmadoConOtraClave_devuelve401() throws Exception {
        String ajeno = Jwts.builder()
                .subject("admin")
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor("otra-clave-que-el-servidor-no-conoce-12345".getBytes()))
                .compact();

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(ajeno)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token invalido"));
    }

    @Test
    void compradorNoPuedePublicarNiGestionar_devuelve403ConJson() throws Exception {
        String comprador = registrarYLoguearComprador("compradorrol");

        mockMvc.perform(post("/api/vuelos").header("Authorization", bearer(comprador))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("No tiene permisos para realizar esta accion"));
        mockMvc.perform(patch("/api/vuelos/1/estado").header("Authorization", bearer(comprador))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"DEMORADO\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/descuentos").header("Authorization", bearer(comprador))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/disponibilidades").header("Authorization", bearer(comprador))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendedorNoPuedeUsarCarritoNiOrdenes_devuelve403() throws Exception {
        String vendedor = registrarYLoguearAdmin("vendedorrol");

        mockMvc.perform(get("/api/carrito").header("Authorization", bearer(vendedor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", bearer(vendedor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/ordenes").header("Authorization", bearer(vendedor)))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloAdminAdministraUsuariosYDaAltaDeAdministradores() throws Exception {
        String otroComprador = registrarYLoguearComprador("vendedoradmin");
        String comprador = registrarYLoguearComprador("compradoradmin");
        String admin = loginAdmin();

        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(otroComprador)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/usuarios/1/rol").header("Authorization", bearer(comprador))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/registro/administrador").header("Authorization", bearer(otroComprador))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void catalogoYConsultas_sonPublicas() throws Exception {
        mockMvc.perform(get("/api/vuelos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categorias")).andExpect(status().isOk());
        mockMvc.perform(get("/api/aeropuertos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/clases")).andExpect(status().isOk());
    }

    @Test
    void rutasDeUsuarioLogueado_pidenToken_peroNoRol() throws Exception {
        String comprador = registrarYLoguearComprador("cualquierrol");
        String vendedor = registrarYLoguearAdmin("cualquierrol2");

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(comprador))).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(vendedor))).andExpect(status().isOk());
    }
}
