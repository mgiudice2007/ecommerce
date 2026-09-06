package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.FotoRequest;
import com.ecommerce.vuelos.dto.vuelo.FotoResponse;
import com.ecommerce.vuelos.entity.Foto;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FotoService {

    FotoResponse subir(FotoRequest request, MultipartFile archivo, UsuarioPrincipal principal);

    List<FotoResponse> listarPorVuelo(Long vueloId);

    Foto obtenerBinario(Long id);

    void eliminar(Long id, UsuarioPrincipal principal);
}
