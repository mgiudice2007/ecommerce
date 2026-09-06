package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CarritoCheckoutIntegrationTest extends IntegrationTestSupport {


    @Test
    void compradorDelSeeder_naceConCarrito() throws Exception {
        String comprador = login("comprador", "comprador123");

        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void agregarItem_superandoElStockDisponible_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 100.0, 2);
        String pasajero = registrarYLoguearComprador("pasajerostock");

        mockMvc.perform(json(post("/api/carrito/items").header("Authorization", "Bearer " + pasajero), Map.of("disponibilidadId", cupoId, "cantidad", 3)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agregarElMismoVueloDosVeces_acumulaLaCantidad() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 100.0, 10);
        String pasajero = registrarYLoguearComprador("pasajeroacumula");

        agregarAlCarrito(pasajero, cupoId, 2);
        mockMvc.perform(json(post("/api/carrito/items").header("Authorization", "Bearer " + pasajero), Map.of("disponibilidadId", cupoId, "cantidad", 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].cantidad").value(5))
                .andExpect(jsonPath("$.total").value(500.0));
    }

    @Test
    void actualizarYEliminarItemDelCarrito() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 100.0, 10);
        String pasajero = registrarYLoguearComprador("pasajeroupdateitem");
        Long itemId = agregarAlCarrito(pasajero, cupoId, 1);

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
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 200.0, 10);
        String pasajero = registrarYLoguearComprador("pasajerocheckout");
        agregarAlCarrito(pasajero, cupoId, 4);

        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.total").value(800.0));

        mockMvc.perform(get("/api/disponibilidades/" + cupoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asientosDisponibles").value(6));

        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void checkout_siElStockBajaDespuesDeAgregarAlCarrito_fallaYNoDescuentaNadaDeNingunVuelo() throws Exception {
        String vendedor = registrarYLoguearVendedor("v" + System.nanoTime() % 100000);
        Long cupoOk = crearVueloConCupo(vendedor, "EZE", "MAD", 100.0, 10);
        Long cupoSinStock = crearVueloConCupo(vendedor, "EZE", "MAD", 100.0, 5);

        String pasajero = registrarYLoguearComprador("pasajeroatomico");
        agregarAlCarrito(pasajero, cupoOk, 2);
        agregarAlCarrito(pasajero, cupoSinStock, 3);

        // Se vende el stock de cupoSinStock por otro medio despues de que ya estaba en el carrito
        Map<String, Object> bajaDeStock = Map.of(
                "vueloId", 0,
                "claseId", clasePorDefecto(),
                "asientosTotales", 1,
                "precio", 100.0);

        mockMvc.perform(json(put("/api/disponibilidades/" + cupoSinStock)
                        .header("Authorization", "Bearer " + vendedor), bajaDeStock))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isBadRequest());

        // cupoOk no debe haber sido descontado a pesar de tener stock suficiente
        mockMvc.perform(get("/api/disponibilidades/" + cupoOk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asientosDisponibles").value(10));
    }
}
