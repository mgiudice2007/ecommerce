package com.ecommerce.vuelos.config;

import com.ecommerce.vuelos.entity.Aeropuerto;
import com.ecommerce.vuelos.entity.Categoria;
import com.ecommerce.vuelos.entity.Clase;
import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.EstadoVuelo;
import com.ecommerce.vuelos.entity.Rol;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.repository.AeropuertoRepository;
import com.ecommerce.vuelos.repository.CategoriaRepository;
import com.ecommerce.vuelos.repository.ClaseRepository;
import com.ecommerce.vuelos.repository.DisponibilidadRepository;
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
    private final ClaseRepository claseRepository;
    private final DisponibilidadRepository disponibilidadRepository;
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

        if (claseRepository.count() == 0) {
            claseRepository.saveAll(List.of(
                    clase("Economica", "Asiento estandar", false),
                    clase("Ejecutiva", "Mayor espacio y comidas", true),
                    clase("Primera", "Cabina privada", true)));
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

            Vuelo aMadrid = vueloRepository.save(Vuelo.builder()
                    .vendedor(vendedor)
                    .categoria(internacional)
                    .origen(eze)
                    .destino(mad)
                    .numeroVuelo("AR1130")
                    .descripcion("Vuelo directo Buenos Aires - Madrid")
                    .fechaSalida(LocalDateTime.now().plusDays(30))
                    .fechaLlegada(LocalDateTime.now().plusDays(30).plusHours(12))
                    .precio(new BigDecimal("950.00"))
                    .descuento(BigDecimal.ZERO)
                    .estado(EstadoVuelo.ACTIVO)
                    .fechaAlta(LocalDateTime.now())
                    .build());

            Vuelo aCordoba = vueloRepository.save(Vuelo.builder()
                    .vendedor(vendedor)
                    .categoria(cabotaje)
                    .origen(aep)
                    .destino(cor)
                    .numeroVuelo("AR2400")
                    .descripcion("Buenos Aires - Cordoba, con 10% de descuento")
                    .fechaSalida(LocalDateTime.now().plusDays(15))
                    .fechaLlegada(LocalDateTime.now().plusDays(15).plusHours(2))
                    .precio(new BigDecimal("700.00"))
                    .descuento(new BigDecimal("10"))
                    .estado(EstadoVuelo.ACTIVO)
                    .fechaAlta(LocalDateTime.now())
                    .build());

            Clase economica = claseRepository.findByNombre("Economica").orElseThrow();
            Clase ejecutiva = claseRepository.findByNombre("Ejecutiva").orElseThrow();

            disponibilidadRepository.saveAll(List.of(
                    cupo(aMadrid, economica, 120, new BigDecimal("950.00")),
                    cupo(aMadrid, ejecutiva, 20, new BigDecimal("2100.00")),
                    cupo(aCordoba, economica, 80, new BigDecimal("700.00"))));
        }
    }

    private Clase clase(String nombre, String descripcion, boolean equipajeBodega) {
        return Clase.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .equipajeBodega(equipajeBodega)
                .build();
    }

    private Disponibilidad cupo(Vuelo vuelo, Clase clase, int asientos, BigDecimal precio) {
        return Disponibilidad.builder()
                .vuelo(vuelo)
                .clase(clase)
                .asientosTotales(asientos)
                .asientosDisponibles(asientos)
                .precio(precio)
                .build();
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
