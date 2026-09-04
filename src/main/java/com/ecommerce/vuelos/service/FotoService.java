package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.FotoRequest;
import com.ecommerce.vuelos.dto.vuelo.FotoResponse;
import com.ecommerce.vuelos.entity.Foto;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Las fotos del vuelo. El binario se guarda en la base, como en el material de la catedra. */
public interface FotoService {

    FotoResponse subir(FotoRequest request, MultipartFile archivo, UsuarioPrincipal principal);

    List<FotoResponse> listarPorVuelo(Long vueloId);

    /**
     * Devuelve la entidad y no un DTO porque el controller necesita los bytes
     * crudos para armar la respuesta binaria.
     */
    Foto obtenerBinario(Long id);

    void eliminar(Long id, UsuarioPrincipal principal);
}
