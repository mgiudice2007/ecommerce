package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CarritoCheckoutIntegrationTest extends IntegrationTestSupport {


    private void agregarPasajeros(String token, Long disponibilidadId, int cantidad, String tipo,
                                  int statusEsperado) throws Exception {
        mockMvc.perform(json(post("/api/carrito/items").header("Authorization", "Bearer " + token),
                        Map.of("disponibilidadId", disponibilidadId, "cantidad", cantidad, "tipoPasajero", tipo)))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    void ninosYBebes_paganUnPorcentajeDelPrecioDeUnAdulto() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long disponibilidadId = crearVueloConCupo(vendedor, "EZE", "MIA", 1000.0, 10);
        String comprador = registrarYLoguearComprador("familia");

        agregarPasajeros(comprador, disponibilidadId, 2, "ADULTO", 201);
        agregarPasajeros(comprador, disponibilidadId, 1, "NINO", 201);
        agregarPasajeros(comprador, disponibilidadId, 1, "BEBE", 201);

        // 2 adultos x 1000 + 1 nino al 75% + 1 bebe al 10%
        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + comprador))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.total").value(2850.0));

        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(2850.0))
                .andExpect(jsonPath("$.items[?(@.tipoPasajero == 'NINO')].precioUnitario").value(750.0));
    }

    @Test
    void todosLosPasajerosOcupanAsientos_delMismoCupo() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long disponibilidadId = crearVueloConCupo(vendedor, "EZE", "MIA", 1000.0, 2);
        String comprador = registrarYLoguearComprador("sinlugar");

        agregarPasajeros(comprador, disponibilidadId, 2, "ADULTO", 201);
        agregarPasajeros(comprador, disponibilidadId, 1, "NINO", 400);
    }

    /** El multiplicador de millas sale del catalogo, no lo suponemos en el test. */
    private int multiplicadorDeLaClasePorDefecto() throws Exception {
        String clases = mockMvc.perform(get("/api/clases")).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(clases).get(0).get("multiplicadorMillas").asInt();
    }

    private int saldoDeMillas(String token) throws Exception {
        String perfil = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(perfil).get("millas").asInt();
    }

    private Long comprar(String token, Integer millas) throws Exception {
        var pedido = post("/api/carrito/checkout").header("Authorization", "Bearer " + token);
        String respuesta = mockMvc.perform(millas == null ? pedido : json(pedido, Map.of("millas", millas)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("id").asLong();
    }

    @Test
    void comprar_sumaMillasSegunLoPagadoYLaClase() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long disponibilidadId = crearVueloConCupo(vendedor, "EZE", "MIA", 1000.0, 10);
        String comprador = registrarYLoguearComprador("millero");
        int multiplicador = multiplicadorDeLaClasePorDefecto();

        agregarAlCarrito(comprador, disponibilidadId, 2);
        // $2000 pagados: 1 milla cada $100, por el multiplicador de la clase
        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + comprador))
                .andExpect(jsonPath("$.millasAGanar").value(20 * multiplicador))
                .andExpect(jsonPath("$.millasDisponibles").value(0));

        comprar(comprador, null);
        org.junit.jupiter.api.Assertions.assertEquals(20 * multiplicador, saldoDeMillas(comprador));
    }

    @Test
    void usarMillas_descuentaDelTotal_yCancelarLasDevuelve() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long caro = crearVueloConCupo(vendedor, "EZE", "MIA", 100000.0, 10);
        Long barato = crearVueloConCupo(vendedor, "EZE", "MIA", 1000.0, 10);
        String comprador = registrarYLoguearComprador("canjeador");

        // Primera compra: junta millas
        agregarAlCarrito(comprador, caro, 1);
        comprar(comprador, null);
        int saldoInicial = saldoDeMillas(comprador);

        // Segunda compra: paga $500 con millas y el resto con plata
        agregarAlCarrito(comprador, barato, 1);
        Long ordenId = comprar(comprador, 500);
        mockMvc.perform(get("/api/ordenes/" + ordenId).header("Authorization", "Bearer " + comprador))
                .andExpect(jsonPath("$.total").value(500.0))
                .andExpect(jsonPath("$.millasUsadas").value(500))
                .andExpect(jsonPath("$.descuentoMillas").value(500.0));

        // Al cancelar vuelve al saldo que tenia antes de esa compra
        mockMvc.perform(post("/api/ordenes/" + ordenId + "/cancelar").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(saldoInicial, saldoDeMillas(comprador));
    }

    @Test
    void usarMasMillasDeLasQueTiene_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long disponibilidadId = crearVueloConCupo(vendedor, "EZE", "MIA", 1000.0, 10);
        String comprador = registrarYLoguearComprador("sinmillas");

        agregarAlCarrito(comprador, disponibilidadId, 1);
        mockMvc.perform(json(post("/api/carrito/checkout").header("Authorization", "Bearer " + comprador),
                        Map.of("millas", 100)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compradorDelSeeder_naceConCarrito() throws Exception {
        String comprador = login("comprador", "comprador123");

        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void cadaCompradorVeSoloSuPropioCarrito() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long disponibilidadId = crearVueloConCupo(vendedor, "EZE", "MIA", 500.0, 10);
        String uno = registrarYLoguearComprador("carritouno");
        String dos = registrarYLoguearComprador("carritodos");

        agregarAlCarrito(uno, disponibilidadId, 2);

        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + uno))
                .andExpect(jsonPath("$.items.length()").value(1));
        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + dos))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void noSePuedeModificarNiBorrarElItemDeOtroComprador() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long disponibilidadId = crearVueloConCupo(vendedor, "EZE", "MIA", 500.0, 10);
        String dueno = registrarYLoguearComprador("carritodueno");
        String intruso = registrarYLoguearComprador("carritointruso");

        Long itemDelDueno = agregarAlCarrito(dueno, disponibilidadId, 2);

        mockMvc.perform(put("/api/carrito/items/" + itemDelDueno)
                        .header("Authorization", "Bearer " + intruso)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":5}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/carrito/items/" + itemDelDueno)
                        .header("Authorization", "Bearer " + intruso))
                .andExpect(status().isNotFound());

        // el carrito del dueno quedo intacto
        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + dueno))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].cantidad").value(2));
    }

    @Test
    void elCheckoutDeUnCompradorNoToca_elCarritoDeOtro() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long disponibilidadId = crearVueloConCupo(vendedor, "EZE", "MIA", 500.0, 10);
        String uno = registrarYLoguearComprador("checkoutuno");
        String dos = registrarYLoguearComprador("checkoutdos");

        agregarAlCarrito(uno, disponibilidadId, 2);

        // dos tiene el carrito vacio: su checkout falla y no se lleva el carrito de uno
        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + dos))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/carrito").header("Authorization", "Bearer " + uno))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void carritoYOrdenes_sinToken_devuelven401() throws Exception {
        mockMvc.perform(get("/api/carrito")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/carrito/checkout")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/ordenes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/ordenes/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void agregarItem_superandoElStockDisponible_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long cupoId = crearVueloConCupo(vendedor, "EZE", "MAD", 100.0, 2);
        String pasajero = registrarYLoguearComprador("pasajerostock");

        mockMvc.perform(json(post("/api/carrito/items").header("Authorization", "Bearer " + pasajero), Map.of("disponibilidadId", cupoId, "cantidad", 3)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agregarElMismoVueloDosVeces_acumulaLaCantidad() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
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
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
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
    void agregarItem_deUnVueloQueYaSalio_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long vueloId = crearVuelo(vendedor, "AEP", "COR", 300.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 300.0);
        hacerQueYaSalio(vueloId);
        String pasajero = registrarYLoguearComprador("pasajerotarde");

        mockMvc.perform(post("/api/carrito/items")
                        .header("Authorization", "Bearer " + pasajero)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("disponibilidadId", cupoId, "cantidad", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("ya salio")));
    }

    @Test
    void checkout_conUnVueloQueSalioMientrasEstabaEnElCarrito_devuelve400() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
        Long vueloId = crearVuelo(vendedor, "AEP", "MDZ", 300.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 300.0);
        String pasajero = registrarYLoguearComprador("pasajerodistraido");
        agregarAlCarrito(pasajero, cupoId, 2);

        hacerQueYaSalio(vueloId);

        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isBadRequest());
        // No se descontaron asientos
        mockMvc.perform(get("/api/disponibilidades/" + cupoId))
                .andExpect(jsonPath("$.asientosDisponibles").value(10));
    }

    @Test
    void checkout_conCarritoVacio_devuelve400() throws Exception {
        String pasajero = registrarYLoguearComprador("pasajerocarritovacio");

        mockMvc.perform(post("/api/carrito/checkout").header("Authorization", "Bearer " + pasajero))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_generaOrdenYDescuentaStock() throws Exception {
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
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
        String vendedor = registrarYLoguearAdmin("v" + System.nanoTime() % 100000);
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
