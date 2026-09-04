package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FotoControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void subirFoto_quedaListadaYDescargable() throws Exception {
        String vendedor = registrarYLoguearVendedor("vfoto");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 500.0);

        Long fotoId = subirFoto(vendedor, vueloId);

        mockMvc.perform(get("/api/fotos").param("vueloId", vueloId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombreArchivo").value("portada.jpg"))
                .andExpect(jsonPath("$[0].tamanioBytes").value(4));

        mockMvc.perform(get("/api/fotos/" + fotoId + "/contenido"))
                .andExpect(status().isOk());
    }

    @Test
    void subirArchivoQueNoEsImagen_devuelve400() throws Exception {
        String vendedor = registrarYLoguearVendedor("vfotoinvalida");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 500.0);

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "documento.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/fotos")
                        .file(archivo)
                        .param("vueloId", vueloId.toString())
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void subirFotoDeOtroVendedor_devuelve400() throws Exception {
        String dueno = registrarYLoguearVendedor("vpropietariofoto");
        Long vueloId = crearVuelo(dueno, "EZE", "MAD", 500.0);

        String intruso = registrarYLoguearVendedor("vintrusofoto");
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "portada.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});

        mockMvc.perform(multipart("/api/fotos")
                        .file(archivo)
                        .param("vueloId", vueloId.toString())
                        .header("Authorization", "Bearer " + intruso))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarFoto_comoDueno_devuelveNoContent() throws Exception {
        String vendedor = registrarYLoguearVendedor("vfotoeliminar");
        Long vueloId = crearVuelo(vendedor, "EZE", "MAD", 500.0);
        Long fotoId = subirFoto(vendedor, vueloId);

        mockMvc.perform(delete("/api/fotos/" + fotoId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/fotos").param("vueloId", vueloId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
