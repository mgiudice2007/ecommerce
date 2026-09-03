package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CarritoCheckoutIntegrationTest extends IntegrationTestSupport {

    @Test
    void agregarItem_superandoElStockDisponible_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0, 2, "ECONOMICA");
        String pasajero = registrarYLoguearComprador("pasajerostock");

        mockMvc.perform(json(post("/api/carrito/items").header("Authorization", "Bearer " + pasajero), Map.of("vueloId", vueloId, "cantidad", 3)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agregarElMismoVueloDosVeces_acumulaLaCantidad() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0, 10, "ECONOMICA");
        String pasajero = registrarYLoguearComprador("pasajeroacumula");

        agregarAlCarrito(pasajero, vueloId, 2);
        mockMvc.perform(json(post("/api/carrito/items").header("Authorization", "Bearer " + pasajero), Map.of("vueloId", vueloId, "cantidad", 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].cantidad").value(5))
                .andExpect(jsonPath("$.total").value(500.0));
    }

    @Test
    void actualizarYEliminarItemDelCarrito() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 100.0, 10, "ECONOMICA");
        String pasajero = registrarYLoguearComprador("pasajeroupdateitem");
        Long itemId = agregarAlCarrito(pasajero, vueloId, 1);

        mockMvc.perform(json(put("/api/carrito/items/" + itemId).header("Authorization", "Bearer " + pasajero), Map.of("cantidad", 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].cantidad").value(4));

        mockMvc.perform(delete("/api/carrito/items/" + itemId).header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void checkout_conCarritoVacio_devuelve400() throws Exception {
        String pasajero = registrarYLoguearComprador("pasajerocarritovacio");

        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_generaOrdenYDescuentaStock() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 200.0, 10, "ECONOMICA");
        String pasajero = registrarYLoguearComprador("pasajerocheckout");
        agregarAlCarrito(pasajero, vueloId, 4);

        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.total").value(800.0));

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asientosDisponibles").value(6));

        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void checkout_siElStockBajaDespuesDeAgregarAlCarrito_fallaYNoDescuentaNadaDeNingunVuelo() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long vueloOk = crearVuelo(vendedor, "EZE", "MAD", 100.0, 10, "ECONOMICA");
        Long vueloSinStock = crearVuelo(vendedor, "EZE", "MAD", 100.0, 5, "ECONOMICA");

        String pasajero = registrarYLoguearComprador("pasajeroatomico");
        agregarAlCarrito(pasajero, vueloOk, 2);
        agregarAlCarrito(pasajero, vueloSinStock, 3);

        // Se vende el stock de vueloSinStock por otro medio despues de que ya estaba en el carrito
        Map<String, Object> bajaDeStock = new LinkedHashMap<>();
        bajaDeStock.put("numeroVuelo", "TSATOMICO");
        bajaDeStock.put("descripcion", "Se quedo casi sin lugar");
        bajaDeStock.put("categoriaId", categoriaPorDefecto());
        bajaDeStock.put("origenIata", "EZE");
        bajaDeStock.put("destinoIata", "MAD");
        bajaDeStock.put("fechaSalida", LocalDateTime.now().plusDays(10).withNano(0).toString());
        bajaDeStock.put("fechaLlegada", LocalDateTime.now().plusDays(10).plusHours(3).withNano(0).toString());
        bajaDeStock.put("precio", 100.0);
        bajaDeStock.put("asientosDisponibles", 1);
        bajaDeStock.put("descuento", 0);
        bajaDeStock.put("clase", "ECONOMICA");

        mockMvc.perform(json(put("/api/vuelos/" + vueloSinStock)
                        .header("Authorization", "Bearer " + vendedor), bajaDeStock))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isBadRequest());

        // vueloOk no debe haber sido descontado a pesar de tener stock suficiente
        mockMvc.perform(get("/api/vuelos/" + vueloOk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asientosDisponibles").value(10));
    }
}
