package com.ecommerce.vuelos.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Autowired
    private RestSecurityHandlers restSecurityHandlers;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService,
                                                             UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth
                        // Todas las reglas de acceso viven aca, de arriba hacia abajo: gana la primera que coincide.
                        // Publico: alta, login y la consulta del catalogo.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vuelos/**", "/api/categorias/**", "/api/aeropuertos/**",
                                "/api/clases", "/api/disponibilidades/**", "/api/fotos/**", "/api/descuentos/**").permitAll()
                        // Solo ADMIN: dar de alta administradores y gestionar usuarios y roles.
                        .requestMatchers("/api/auth/registro/administrador", "/api/usuarios/**").hasRole("ADMIN")
                        // Solo COMPRADOR: su carrito y sus ordenes (el dueno sale siempre del token).
                        .requestMatchers("/api/carrito/**", "/api/ordenes/**").hasRole("COMPRADOR")
                        // VENDEDOR o ADMIN: publicar y gestionar vuelos, cupos, descuentos y fotos.
                        // Que sea el dueno del vuelo lo valida cada servicio.
                        .requestMatchers("/api/vuelos/**", "/api/disponibilidades/**", "/api/descuentos/**",
                                "/api/fotos/**").hasAnyRole("VENDEDOR", "ADMIN")
                        // Cualquier otra ruta (por ejemplo /api/auth/me y /logout) pide estar logueado.
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restSecurityHandlers)
                        .accessDeniedHandler(restSecurityHandlers)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
