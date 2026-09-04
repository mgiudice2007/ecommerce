package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.vuelo.FotoRequest;
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
@RequiredArgsConstructor
public class FotoController {

    private final FotoService fotoService;

    /**
     * Carga una foto del vuelo. No es JSON: es multipart/form-data, con el
     * archivo en la parte "file" y el resto de los campos como partes sueltas.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMIN')")
    public ResponseEntity<FotoResponse> subir(@ModelAttribute FotoRequest request,
                                              @RequestParam("file") MultipartFile file,
                                              @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fotoService.subir(request, file, principal));
    }

    /** Solo los datos de las fotos, sin los binarios. */
    @GetMapping
    public ResponseEntity<List<FotoResponse>> listar(@RequestParam Long vueloId) {
        return ResponseEntity.ok(fotoService.listarPorVuelo(vueloId));
    }

    /**
     * Devuelve la imagen en crudo, con su Content-Type, para que el cliente la
     * pueda mostrar directo en vez de tener que decodificar un Base64.
     */
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

    /**
     * La entidad guarda el nombre y los bytes, no el content type, asi que se
     * deduce de la extension. Es la misma forma de la entidad Product del
     * material de la catedra.
     */
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
