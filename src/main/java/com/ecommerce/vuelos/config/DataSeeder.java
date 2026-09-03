package com.ecommerce.vuelos.config;

import com.ecommerce.vuelos.entity.Aeropuerto;
import com.ecommerce.vuelos.entity.Categoria;
import com.ecommerce.vuelos.entity.ClaseVuelo;
import com.ecommerce.vuelos.entity.EstadoVuelo;
import com.ecommerce.vuelos.entity.Rol;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.repository.AeropuertoRepository;
import com.ecommerce.vuelos.repository.CategoriaRepository;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final AeropuertoRepository aeropuertoRepository;
    private final CategoriaRepository categoriaRepository;
    private final VueloRepository vueloRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (aeropuertoRepository.count() == 0) {
            aeropuertoRepository.saveAll(List.of(
                    aeropuerto("EZE", "Ministro Pistarini", "Buenos Aires", "Buenos Aires", "Argentina"),
                    aeropuerto("AEP", "Jorge Newbery", "Buenos Aires", "Buenos Aires", "Argentina"),
                    aeropuerto("COR", "Ingeniero Taravella", "Cordoba", "Cordoba", "Argentina"),
                    aeropuerto("MDZ", "El Plumerillo", "Mendoza", "Mendoza", "Argentina"),
                    aeropuerto("MAD", "Barajas", "Madrid", "Madrid", "España"),
                    aeropuerto("MIA", "Miami International", "Miami", "Florida", "Estados Unidos")));
        }

        if (categoriaRepository.count() == 0) {
            categoriaRepository.saveAll(List.of(
                    categoria("Cabotaje", "Vuelos dentro del pais"),
                    categoria("Regional", "Vuelos dentro de America"),
                    categoria("Internacional", "Vuelos intercontinentales")));
        }

        if (usuarioRepository.count() == 0) {
            usuarioRepository.save(usuario("admin", "admin@vuelos.com", "Admin", "Sistema", Rol.ADMIN));
            usuarioRepository.save(usuario("vendedor", "vendedor@vuelos.com", "Vane", "Vendedora", Rol.VENDEDOR));
            usuarioRepository.save(usuario("comprador", "comprador@vuelos.com", "Caro", "Compradora", Rol.COMPRADOR));
        }

        if (vueloRepository.count() == 0) {
            Usuario vendedor = usuarioRepository.findByUsername("vendedor").orElseThrow();
            Categoria internacional = categoriaRepository.findByNombre("Internacional").get(0);
            Categoria cabotaje = categoriaRepository.findByNombre("Cabotaje").get(0);
            Aeropuerto eze = aeropuertoRepository.findById("EZE").orElseThrow();
            Aeropuerto mad = aeropuertoRepository.findById("MAD").orElseThrow();
            Aeropuerto aep = aeropuertoRepository.findById("AEP").orElseThrow();
            Aeropuerto cor = aeropuertoRepository.findById("COR").orElseThrow();

            vueloRepository.save(Vuelo.builder()
                    .vendedor(vendedor)
                    .categoria(internacional)
                    .origen(eze)
                    .destino(mad)
                    .numeroVuelo("AR1130")
                    .descripcion("Vuelo directo Buenos Aires - Madrid")
                    .fechaSalida(LocalDateTime.now().plusDays(30))
                    .fechaLlegada(LocalDateTime.now().plusDays(30).plusHours(12))
                    .precio(new BigDecimal("950.00"))
                    .asientosDisponibles(120)
                    .descuento(BigDecimal.ZERO)
                    .clase(ClaseVuelo.ECONOMICA)
                    .estado(EstadoVuelo.ACTIVO)
                    .fechaAlta(LocalDateTime.now())
                    .build());

            vueloRepository.save(Vuelo.builder()
                    .vendedor(vendedor)
                    .categoria(cabotaje)
                    .origen(aep)
                    .destino(cor)
                    .numeroVuelo("AR2400")
                    .descripcion("Buenos Aires - Cordoba, con 10% de descuento")
                    .fechaSalida(LocalDateTime.now().plusDays(15))
                    .fechaLlegada(LocalDateTime.now().plusDays(15).plusHours(2))
                    .precio(new BigDecimal("700.00"))
                    .asientosDisponibles(80)
                    .descuento(new BigDecimal("10"))
                    .clase(ClaseVuelo.EJECUTIVA)
                    .estado(EstadoVuelo.ACTIVO)
                    .fechaAlta(LocalDateTime.now())
                    .build());
        }
    }

    private Aeropuerto aeropuerto(String iata, String nombre, String ciudad, String provincia, String pais) {
        return Aeropuerto.builder()
                .codigoIata(iata)
                .nombre(nombre)
                .ciudad(ciudad)
                .provincia(provincia)
                .pais(pais)
                .zonaHoraria("America/Argentina/Buenos_Aires")
                .build();
    }

    private Categoria categoria(String nombre, String descripcion) {
        return Categoria.builder().nombre(nombre).descripcion(descripcion).build();
    }

    private Usuario usuario(String username, String mail, String nombre, String apellido, Rol rol) {
        return Usuario.builder()
                .username(username)
                .mail(mail)
                .password(passwordEncoder.encode(username + "123"))
                .nombre(nombre)
                .apellido(apellido)
                .rol(rol)
                .build();
    }
}
