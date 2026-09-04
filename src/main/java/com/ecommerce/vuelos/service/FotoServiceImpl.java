package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.FotoResponse;
import com.ecommerce.vuelos.entity.Foto;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.FotoRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FotoServiceImpl implements FotoService {

    private final FotoRepository fotoRepository;
    private final VueloRepository vueloRepository;

    @Override
    public List<FotoResponse> listarPorVuelo(Long vueloId) {
        return fotoRepository.findByVueloIdOrderByOrdenAsc(vueloId).stream()
                .map(FotoResponse::desde)
                .toList();
    }

    @Override
    public Foto obtenerParaDescarga(Long id) {
        return buscarPorId(id);
    }

    @Override
    @Transactional
    public FotoResponse subir(Long vueloId, MultipartFile archivo, Integer orden, UsuarioPrincipal principal) {
        Vuelo vuelo = buscarVuelo(vueloId);
        validarPropiedad(vuelo, principal);

        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo esta vacio");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Solo se permiten imagenes");
        }

        byte[] datos;
        try {
            datos = archivo.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("No se pudo leer el archivo");
        }

        Foto foto = Foto.builder()
                .vuelo(vuelo)
                .nombreArchivo(archivo.getOriginalFilename())
                .datos(datos)
                .orden(orden != null ? orden : (int) fotoRepository.countByVueloId(vueloId))
                .build();

        return FotoResponse.desde(fotoRepository.save(foto));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UsuarioPrincipal principal) {
        Foto foto = buscarPorId(id);
        validarPropiedad(foto.getVuelo(), principal);
        fotoRepository.delete(foto);
    }

    private Foto buscarPorId(Long id) {
        return fotoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada: " + id));
    }

    private Vuelo buscarVuelo(Long id) {
        return vueloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vuelo no encontrado: " + id));
    }

    private void validarPropiedad(Vuelo vuelo, UsuarioPrincipal principal) {
        if (principal.esAdmin()) {
            return;
        }
        if (!vuelo.getVendedor().getId().equals(principal.getId())) {
            throw new BadRequestException("Solo el vendedor que publico el vuelo puede manejar sus fotos");
        }
    }
}
