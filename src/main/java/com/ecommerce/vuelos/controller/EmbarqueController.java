package com.ecommerce.vuelos.controller;

import com.ecommerce.vuelos.dto.embarque.EmbarqueResponse;
import com.ecommerce.vuelos.service.EmbarqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Validacion de pases de embarque: solo para el administrador (el vendedor de la aerolinea). */
@RestController
@RequestMapping("/api/embarque")
public class EmbarqueController {

    @Autowired
    private EmbarqueService embarqueService;

    @GetMapping("/{codigo}")
    public ResponseEntity<EmbarqueResponse> validar(@PathVariable String codigo) {
        return ResponseEntity.ok(embarqueService.validar(codigo));
    }

    @PostMapping("/{codigo}/embarcar")
    public ResponseEntity<EmbarqueResponse> embarcar(@PathVariable String codigo) {
        return ResponseEntity.ok(embarqueService.embarcar(codigo));
    }
}
