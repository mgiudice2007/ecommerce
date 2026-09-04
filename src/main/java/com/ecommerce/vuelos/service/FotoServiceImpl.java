package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.vuelo.FotoRequest;
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

    /** Tope defensivo: el vuelo no necesita mas que esto y evita llenar la base. */
    private static final int MAX_FOTOS_POR_VUELO = 5;

    private final FotoRepository fotoRepository;
    private final VueloRepository vueloRepository;

    /**
     * El mismo par que usa el material de multipart de la catedra:
     * getOriginalFilename() para el nombre y getBytes() para el contenido.
     */
    @Override
    @Transactional
    public FotoResponse subir(FotoRequest request, MultipartFile archivo, UsuarioPrincipal principal) {
        if (request.getVueloId() == null) {
            throw new BadRequestException("Falta el vueloId");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("No llego ningun archivo en la parte 'file'");
        }

        Vuelo vuelo = vueloRepository.findById(request.getVueloId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vuelo no encontrado: " + request.getVueloId()));
        validarPropiedad(vuelo, principal);
        validarEsImagen(archivo);

        long cargadas = fotoRepository.countByVueloId(vuelo.getId());
        if (cargadas >= MAX_FOTOS_POR_VUELO) {
            throw new BadRequestException(
                    "El vuelo ya tiene " + MAX_FOTOS_POR_VUELO + " fotos, que es el maximo");
        }

        byte[] datos;
        try {
            datos = archivo.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("No se pudo leer el archivo: " + e.getMessage());
        }

        Foto foto = Foto.builder()
                .vuelo(vuelo)
                .nombreArchivo(archivo.getOriginalFilename())
                .datos(datos)
                // Si no mandan orden, la foto va al final. La de orden mas bajo es la portada.
                .orden(request.getOrden() != null ? request.getOrden() : (int) cargadas)
                .build();

        return FotoResponse.desde(fotoRepository.save(foto));
    }

    @Override
    public List<FotoResponse> listarPorVuelo(Long vueloId) {
        return fotoRepository.findByVueloIdOrderByOrdenAsc(vueloId).stream()
                .map(FotoResponse::desde)
                .toList();
    }

    @Override
    public Foto obtenerBinario(Long id) {
        return fotoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada: " + id));
    }

    @Override
    @Transactional
    public void eliminar(Long id, UsuarioPrincipal principal) {
        Foto foto = obtenerBinario(id);
        validarPropiedad(foto.getVuelo(), principal);
        // Borrado fisico: a diferencia del vuelo, ninguna orden referencia una foto.
        fotoRepository.delete(foto);
    }

    /**
     * El content type lo manda el cliente, asi que no alcanza para confiar, pero
     * sirve para frenar el error obvio de subir un PDF o un .exe.
     */
    private void validarEsImagen(MultipartFile archivo) {
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new BadRequestException("El archivo tiene que ser una imagen, llego: " + tipo);
        }
    }

    private void validarPropiedad(Vuelo vuelo, UsuarioPrincipal principal) {
        if (principal.esAdmin()) {
            return;
        }
        if (!vuelo.getVendedor().getId().equals(principal.getId())) {
            throw new BadRequestException("Solo el vendedor que publico el vuelo puede cargarle fotos");
        }
    }
}
