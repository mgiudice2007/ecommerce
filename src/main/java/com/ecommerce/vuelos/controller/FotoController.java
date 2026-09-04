package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.vuelo.FotoResponse;
import com.ecommerce.vuelos.entity.Foto;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.FotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
public class FotoController {

    private final FotoService fotoService;

    @GetMapping("/api/fotos")
    public ResponseEntity<List<FotoResponse>> listar(@RequestParam Long vueloId) {
        return ResponseEntity.ok(fotoService.listarPorVuelo(vueloId));
    }

    @GetMapping("/api/fotos/{id}/contenido")
    public ResponseEntity<byte[]> contenido(@PathVariable Long id) {
        Foto foto = fotoService.obtenerParaDescarga(id);
        return ResponseEntity.ok()
                .contentType(contentTypeDe(foto.getNombreArchivo()))
                .body(foto.getDatos());
    }

    @PostMapping(value = "/api/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<FotoResponse> subir(@RequestParam Long vueloId,
                                               @RequestParam(required = false) Integer orden,
                                               @RequestParam MultipartFile archivo,
                                               @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fotoService.subir(vueloId, archivo, orden, principal));
    }

    @DeleteMapping("/api/fotos/{id}")
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        fotoService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }

    private MediaType contentTypeDe(String nombreArchivo) {
        String nombre = nombreArchivo != null ? nombreArchivo.toLowerCase(Locale.ROOT) : "";
        if (nombre.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (nombre.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
