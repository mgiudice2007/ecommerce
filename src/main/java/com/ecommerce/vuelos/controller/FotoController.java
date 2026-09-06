package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.vuelo.FotoRequest;
import com.ecommerce.vuelos.dto.vuelo.FotoResponse;
import com.ecommerce.vuelos.entity.Foto;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import com.ecommerce.vuelos.service.FotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/fotos")
public class FotoController {

    @Autowired
    private FotoService fotoService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<FotoResponse> subir(@ModelAttribute FotoRequest request,
                                              @RequestParam("file") MultipartFile file,
                                              @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fotoService.subir(request, file, principal));
    }


    @GetMapping
    public ResponseEntity<List<FotoResponse>> listar(@RequestParam Long vueloId) {
        return ResponseEntity.ok(fotoService.listarPorVuelo(vueloId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> ver(@PathVariable Long id) {
        Foto foto = fotoService.obtenerBinario(id);
        return ResponseEntity.ok()
                .contentType(tipoSegunExtension(foto.getNombreArchivo()))
                .body(foto.getDatos());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        fotoService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }


    private MediaType tipoSegunExtension(String nombreArchivo) {
        String nombre = nombreArchivo == null ? "" : nombreArchivo.toLowerCase(Locale.ROOT);
        if (nombre.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (nombre.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (nombre.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
