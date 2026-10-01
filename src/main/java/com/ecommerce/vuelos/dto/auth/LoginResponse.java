package com.ecommerce.vuelos.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * El login solo devuelve el token. El id, username y rol ya viajan dentro
 * del JWT; el resto de los datos se piden a /api/auth/me cuando hacen falta.
 */
@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
}
