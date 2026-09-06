package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FotoControllerIntegrationTest extends IntegrationTestSupport {


    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    private MockMultipartFile png(String nombre) {
        return new MockMultipartFile("file", nombre, MediaType.IMAGE_PNG_VALUE, PNG_1X1);
    }

    private Long subir(String token, Long vueloId, String nombre, Integer orden) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/fotos");
        request.file(png(nombre))
                .param("vueloId", vueloId.toString())
                .header("Authorization", "Bearer " + token);
        if (orden != null) {
            request.param("orden", orden.toString());
        }

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void subirFoto_comoDuenoDelVuelo_guardaNombreYTamano() throws Exception {
        String vendedor = registrarYLoguearVendedor("vfoto");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 900.0);

        mockMvc.perform(multipart("/api/fotos")
                        .file(png("portada.png"))
                        .param("vueloId", vueloId.toString())
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreArchivo").value("portada.png"))
                .andExpect(jsonPath("$.vueloId").value(vueloId))
                .andExpect(jsonPath("$.orden").value(0))
                .andExpect(jsonPath("$.tamano").value(PNG_1X1.length))
                // el binario no viaja en el DTO
                .andExpect(jsonPath("$.datos").doesNotExist());
    }

    @Test
    void verFoto_devuelveLosBytesTalCualConSuContentType() throws Exception {
        String vendedor = registrarYLoguearVendedor("vbytes");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 900.0);
        Long fotoId = subir(vendedor, vueloId, "imagen.png", null);

        MvcResult result = mockMvc.perform(get("/api/fotos/" + fotoId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();

        assertArrayEquals(PNG_1X1, result.getResponse().getContentAsByteArray(),
                "los bytes que salen tienen que ser los mismos que entraron");
    }

    @Test
    void subirFoto_aUnVueloAjeno_devuelve400() throws Exception {
        String dueno = registrarYLoguearVendedor("vduenofoto");
        Long vueloId = crearVuelo(dueno, "EZE", "MAD", 900.0);

        String intruso = registrarYLoguearVendedor("vintrusofoto");
        mockMvc.perform(multipart("/api/fotos")
                        .file(png("ajena.png"))
                        .param("vueloId", vueloId.toString())
                        .header("Authorization", "Bearer " + intruso))
                .andExpect(status().isBadRequest());
    }

    @Test
    void subirAlgoQueNoEsImagen_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("vpdf");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 900.0);

        mockMvc.perform(multipart("/api/fotos")
                        .file(new MockMultipartFile("file", "documento.pdf",
                                MediaType.APPLICATION_PDF_VALUE, "no soy una imagen".getBytes()))
                        .param("vueloId", vueloId.toString())
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void subirFoto_sinSerVendedor_devuelve403() throws Exception {
        String vendedor = registrarYLoguearVendedor("vparacomprador");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 900.0);
        String comprador = registrarYLoguearComprador("cfoto");

        mockMvc.perform(multipart("/api/fotos")
                        .file(png("nope.png"))
                        .param("vueloId", vueloId.toString())
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarFotos_vienenOrdenadasYSinBinario() throws Exception {
        String vendedor = registrarYLoguearVendedor("vlista");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 900.0);
        subir(vendedor, vueloId, "segunda.png", 2);
        subir(vendedor, vueloId, "primera.png", 1);

        mockMvc.perform(get("/api/fotos").param("vueloId", vueloId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombreArchivo").value("primera.png"))
                .andExpect(jsonPath("$[1].nombreArchivo").value("segunda.png"))
                .andExpect(jsonPath("$[0].datos").doesNotExist());
    }

    @Test
    void eliminarFoto_laSacaDelListado() throws Exception {
        String vendedor = registrarYLoguearVendedor("vborra");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 900.0);
        Long fotoId = subir(vendedor, vueloId, "sobra.png", null);

        mockMvc.perform(delete("/api/fotos/" + fotoId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/fotos").param("vueloId", vueloId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/fotos/" + fotoId))
                .andExpect(status().isNotFound());
    }
}
