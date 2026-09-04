package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.FotoResponse;
import com.ecommerce.vuelos.entity.Foto;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FotoService {

    List<FotoResponse> listarPorVuelo(Long vueloId);

    /** Entidad completa (con el binario), para descargarla en el controller. */
    Foto obtenerParaDescarga(Long id);

    FotoResponse subir(Long vueloId, MultipartFile archivo, Integer orden, UsuarioPrincipal principal);

    void eliminar(Long id, UsuarioPrincipal principal);
}
