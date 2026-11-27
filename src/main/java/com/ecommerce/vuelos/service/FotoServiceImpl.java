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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class FotoServiceImpl implements FotoService {

    private static final int MAX_FOTOS_POR_VUELO = 5;

    @Autowired
    private FotoRepository fotoRepository;
    @Autowired
    private VueloRepository vueloRepository;

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
        validarContenidoDeImagen(datos);

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

    private void validarEsImagen(MultipartFile archivo) {
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new BadRequestException("El archivo tiene que ser una imagen, llego: " + tipo);
        }
    }

    /**
     * El tipo lo informa el navegador y se puede falsear, asi que tambien se mira como empieza el archivo:
     * cada formato de imagen (JPG, PNG, GIF, WEBP) arranca con sus propios bytes.
     */
    private void validarContenidoDeImagen(byte[] datos) {
        String inicio = new String(datos, 0, Math.min(datos.length, 12), StandardCharsets.ISO_8859_1);
        boolean esImagen = inicio.startsWith("ÿØÿ")
                || inicio.startsWith("\u0089PNG")
                || inicio.startsWith("GIF8")
                || (inicio.startsWith("RIFF") && inicio.startsWith("WEBP", 8));
        if (!esImagen) {
            throw new BadRequestException("El archivo no es una imagen valida (JPG, PNG, GIF o WEBP)");
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
