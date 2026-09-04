package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DescuentoControllerIntegrationTest extends IntegrationTestSupport {

    private Map<String, Object> body(Long vueloId, String tipo, Object valor,
                                     LocalDate desde, LocalDate hasta) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vueloId", vueloId);
        body.put("tipoDescuento", tipo);
        body.put("valor", valor);
        body.put("fechaDesde", desde.toString());
        body.put("fechaHasta", hasta.toString());
        return body;
    }

    /** Una ventana que incluye hoy, asi el descuento queda vigente. */
    private Map<String, Object> vigenteHoy(Long vueloId, String tipo, Object valor) {
        return body(vueloId, tipo, valor, LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));
    }

    private MvcResult crear(String token, Map<String, Object> body, int estadoEsperado) throws Exception {
        return mockMvc.perform(post("/api/descuentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(estadoEsperado))
                .andReturn();
    }

    @Test
    void descuentoPorcentual_vigente_bajaElPrecioDelVuelo() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtoporc");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        crear(vendedor, vigenteHoy(vueloId, "PORCENTAJE", 25), 201);

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(1000.0))
                .andExpect(jsonPath("$.precioConDescuento").value(750.0))
                .andExpect(jsonPath("$.descuentoVigente.tipoDescuento").value("PORCENTAJE"))
                .andExpect(jsonPath("$.descuentoVigente.valor").value(25))
                .andExpect(jsonPath("$.descuentoVigente.vigente").value(true));
    }

    @Test
    void descuentoDeMontoFijo_restaLaPlataDirecto() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtomonto");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        crear(vendedor, vigenteHoy(vueloId, "MONTO_FIJO", 150), 201);

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioConDescuento").value(850.0));
    }

    @Test
    void descuentoFueraDeVigencia_noTocaElPrecio() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtoviejo");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        // Ventana que ya paso: se guarda igual, pero no rige.
        crear(vendedor, body(vueloId, "PORCENTAJE", 50,
                LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1)), 201);

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioConDescuento").value(1000.0))
                .andExpect(jsonPath("$.descuentoVigente").doesNotExist());

        // pero sigue existiendo en el historial del vuelo
        mockMvc.perform(get("/api/descuentos").param("vueloId", vueloId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vigente").value(false));
    }

    @Test
    void dosDescuentosQueSeSolapan_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtosolapa");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        crear(vendedor, body(vueloId, "PORCENTAJE", 10,
                LocalDate.now(), LocalDate.now().plusDays(30)), 201);

        // arranca dentro de la ventana anterior
        crear(vendedor, body(vueloId, "PORCENTAJE", 20,
                LocalDate.now().plusDays(15), LocalDate.now().plusDays(45)), 400);

        // pero uno que arranca despues entra sin problema
        crear(vendedor, body(vueloId, "PORCENTAJE", 20,
                LocalDate.now().plusDays(31), LocalDate.now().plusDays(45)), 201);
    }

    @Test
    void porcentajeMayorA100_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdto101");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);
        crear(vendedor, vigenteHoy(vueloId, "PORCENTAJE", 150), 400);
    }

    @Test
    void montoFijoMayorAlPrecio_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtocaro");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 500.0);
        crear(vendedor, vigenteHoy(vueloId, "MONTO_FIJO", 900), 400);
    }

    @Test
    void fechaHastaAnteriorADesde_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtofechas");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);
        crear(vendedor, body(vueloId, "PORCENTAJE", 10,
                LocalDate.now().plusDays(30), LocalDate.now()), 400);
    }

    @Test
    void cargarDescuentoEnVueloAjeno_devuelve400() throws Exception {
        String dueno = registrarYLoguearVendedor("vdtodueno");
        Long vueloId = crearVuelo(dueno, "EZE", "MAD", 1000.0);

        String intruso = registrarYLoguearVendedor("vdtointruso");
        crear(intruso, vigenteHoy(vueloId, "PORCENTAJE", 10), 400);
    }

    @Test
    void cargarDescuentoComoComprador_devuelve403() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtoparacomp");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);
        String comprador = registrarYLoguearComprador("cdto");

        crear(comprador, vigenteHoy(vueloId, "PORCENTAJE", 10), 403);
    }

    @Test
    void obtenerDescuentoPorId_esPublicoYCalculaSiEstaVigente() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtoporid");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        MvcResult creado = crear(vendedor, vigenteHoy(vueloId, "PORCENTAJE", 15), 201);
        Long descuentoId = objectMapper.readTree(creado.getResponse().getContentAsString())
                .get("id").asLong();

        // sin token: es de lectura publica, como el catalogo
        mockMvc.perform(get("/api/descuentos/" + descuentoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(descuentoId))
                .andExpect(jsonPath("$.vueloId").value(vueloId))
                .andExpect(jsonPath("$.tipoDescuento").value("PORCENTAJE"))
                .andExpect(jsonPath("$.valor").value(15))
                .andExpect(jsonPath("$.vigente").value(true));
    }

    @Test
    void modificarDescuento_cambiaElTipoYRecalculaElPrecio() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtoedita");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        MvcResult creado = crear(vendedor, vigenteHoy(vueloId, "PORCENTAJE", 20), 201);
        Long descuentoId = objectMapper.readTree(creado.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(jsonPath("$.precioConDescuento").value(800.0));

        // de 20% a un monto fijo de 350
        Map<String, Object> aMontoFijo = vigenteHoy(vueloId, "MONTO_FIJO", 350);
        mockMvc.perform(put("/api/descuentos/" + descuentoId)
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aMontoFijo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDescuento").value("MONTO_FIJO"))
                .andExpect(jsonPath("$.valor").value(350));

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(jsonPath("$.precioConDescuento").value(650.0));

        // editar el de otro vendedor no se puede
        String intruso = registrarYLoguearVendedor("vdtoeditaajeno");
        mockMvc.perform(put("/api/descuentos/" + descuentoId)
                        .header("Authorization", "Bearer " + intruso)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aMontoFijo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarDescuento_devuelveElPrecioOriginal() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtoborra");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);

        MvcResult creado = crear(vendedor, vigenteHoy(vueloId, "PORCENTAJE", 20), 201);
        Long descuentoId = objectMapper.readTree(creado.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(jsonPath("$.precioConDescuento").value(800.0));

        mockMvc.perform(delete("/api/descuentos/" + descuentoId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vuelos/" + vueloId))
                .andExpect(jsonPath("$.precioConDescuento").value(1000.0))
                .andExpect(jsonPath("$.descuentoVigente").doesNotExist());
    }

    @Test
    void checkout_congelaElDescuentoAplicadoAunqueDespuesSeApague() throws Exception {
        String vendedor = registrarYLoguearVendedor("vdtocheckout");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 1000.0);
        Long cupoId = crearDisponibilidad(vendedor, vueloId, clasePorDefecto(), 10, 1000.0);

        MvcResult creado = crear(vendedor, vigenteHoy(vueloId, "PORCENTAJE", 30), 201);
        Long descuentoId = objectMapper.readTree(creado.getResponse().getContentAsString())
                .get("id").asLong();

        String comprador = registrarYLoguearComprador("cdtocheckout");
        agregarAlCarrito(comprador, cupoId, 2);

        MvcResult checkout = mockMvc.perform(post("/api/carrito/checkout")
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isCreated())
                // 2 x 700 pagados, 2 x 300 ahorrados
                .andExpect(jsonPath("$.total").value(1400.0))
                .andExpect(jsonPath("$.descuentoTotal").value(600.0))
                .andExpect(jsonPath("$.items[0].precioUnitario").value(700.0))
                .andExpect(jsonPath("$.items[0].descuentoAplicado").value(300.0))
                .andReturn();

        Long ordenId = objectMapper.readTree(checkout.getResponse().getContentAsString())
                .get("id").asLong();

        // El vendedor saca el descuento despues de la compra...
        mockMvc.perform(delete("/api/descuentos/" + descuentoId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isNoContent());

        // ...y la orden ya emitida no se entera: sigue diciendo lo que se pago
        // y lo que se ahorro ese dia.
        mockMvc.perform(get("/api/ordenes/" + ordenId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1400.0))
                .andExpect(jsonPath("$.descuentoTotal").value(600.0))
                .andExpect(jsonPath("$.items[0].descuentoAplicado").value(300.0));
    }
}
