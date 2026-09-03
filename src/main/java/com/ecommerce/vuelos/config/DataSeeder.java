package com.ecommerce.vuelos.config;

import com.ecommerce.vuelos.entity.*;
import com.ecommerce.vuelos.entity.Rol;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import com.ecommerce.vuelos.repository.AerolineaRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final AerolineaRepository aerolineaRepository;
    private final VueloRepository vueloRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            Usuario admin = Usuario.builder()
                    .username("admin")
                    .mail("admin@vuelos.com")
                    .password(passwordEncoder.encode("admin123"))
                    .nombre("Admin")
                    .apellido("Sistema")
                    .rol(Rol.ADMIN)
                    .build();
            usuarioRepository.save(admin);
        }

        if (aerolineaRepository.count() == 0) {
            Aerolinea aerolinea = aerolineaRepository.save(
                    Aerolinea.builder().nombre("Aerolineas Demo").build());

            vueloRepository.save(Vuelo.builder()
                    .origen("Buenos Aires")
                    .destino("Madrid")
                    .fechaSalida(LocalDateTime.now().plusDays(30))
                    .precio(new BigDecimal("950.00"))
                    .asientosDisponibles(120)
                    .descuento(BigDecimal.ZERO)
                    .clase(ClaseVuelo.ECONOMICA)
                    .aerolinea(aerolinea)
                    .build());

            vueloRepository.save(Vuelo.builder()
                    .origen("Buenos Aires")
                    .destino("Miami")
                    .fechaSalida(LocalDateTime.now().plusDays(15))
                    .precio(new BigDecimal("700.00"))
                    .asientosDisponibles(80)
                    .descuento(new BigDecimal("10"))
                    .clase(ClaseVuelo.EJECUTIVA)
                    .aerolinea(aerolinea)
                    .build());
        }
    }
}
