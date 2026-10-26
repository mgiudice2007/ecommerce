package com.ecommerce.vuelos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {


    public static final String JWT_ERROR_ATTRIBUTE = "jwt.error";

    @Autowired
    private ObjectMapper objectMapper;

 
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object errorDeToken = request.getAttribute(JWT_ERROR_ATTRIBUTE);
        escribir(response, HttpServletResponse.SC_UNAUTHORIZED,
                errorDeToken != null ? errorDeToken.toString() : "No autenticado");
    }


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, HttpServletResponse.SC_FORBIDDEN, "No tiene permisos para realizar esta accion");
    }

    private void escribir(HttpServletResponse response, int status, String mensaje) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", mensaje);

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
