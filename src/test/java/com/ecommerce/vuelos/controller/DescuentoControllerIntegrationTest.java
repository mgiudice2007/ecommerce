package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DescuentoControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void crearDescuentoVigente_bajaElPrecioConDescuentoDelVuelo() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdescuento");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        crearDescuento(vendedor, vueloId, "PORCENTAJE", 20,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), true);

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioConDescuento").value(800.0));
    }

    @Test
    void descuentoFueraDeVigencia_noAfectaElPrecio() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdescuentovencido");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        crearDescuento(vendedor, vueloId, "PORCENTAJE", 20,
                LocalDate.now().minusDays(20), LocalDate.now().minusDays(10), true);

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioConDescuento").value(1000.0));
    }

    @Test
    void descuentoMontoFijo_restaElValorDirecto() throws Exception {
        String vendedor = registrarYLoguearVendedor("vmontofijo");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        crearDescuento(vendedor, vueloId, "MONTO_FIJO", 150,
                LocalDate.now(), LocalDate.now().plusDays(5), true);

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioConDescuento").value(850.0));
    }

    @Test
    void dosDescuentosActivosSuperpuestos_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("vsolapado");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        crearDescuento(vendedor, vueloId, "PORCENTAJE", 10,
                LocalDate.now(), LocalDate.now().plusDays(10), true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vueloId", vueloId);
        body.put("tipoDescuento", "PORCENTAJE");
        body.put("valor", 15);
        body.put("fechaDesde", LocalDate.now().plusDays(5).toString());
        body.put("fechaHasta", LocalDate.now().plusDays(15).toString());
        body.put("activo", true);

        mockMvc.perform(post("/api/descuentos")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void porcentajeMayorA100_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("vporcentajealto");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vueloId", vueloId);
        body.put("tipoDescuento", "PORCENTAJE");
        body.put("valor", 150);
        body.put("fechaDesde", LocalDate.now().toString());
        body.put("fechaHasta", LocalDate.now().plusDays(5).toString());
        body.put("activo", true);

        mockMvc.perform(post("/api/descuentos")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearDescuentoDeOtroVendedor_devuelve400() throws Exception {
        String dueno = registrarYLoguearVendedor("vpropietariodesc");
        Long vueloId = crearVuelo(dueno, "EZE", "MAD", 500.0);

        String intruso = registrarYLoguearVendedor("vintrusodesc");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vueloId", vueloId);
        body.put("tipoDescuento", "PORCENTAJE");
        body.put("valor", 10);
        body.put("fechaDesde", LocalDate.now().toString());
        body.put("fechaHasta", LocalDate.now().plusDays(5).toString());
        body.put("activo", true);

        mockMvc.perform(post("/api/descuentos")
                        .header("Authorization", "Bearer " + intruso)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarDescuentosDeUnVuelo_publico() throws Exception {
        String vendedor = registrarYLoguearVendedor("vlistadesc");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 500.0);
        crearDescuento(vendedor, vueloId, "PORCENTAJE", 10,
                LocalDate.now(), LocalDate.now().plusDays(5), true);

        mockMvc.perform(get("/api/descuentos").param("vueloId", vueloId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vigente").value(true));
    }
}
