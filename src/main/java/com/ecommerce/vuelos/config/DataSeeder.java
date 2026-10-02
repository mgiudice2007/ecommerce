package com.ecommerce.vuelos.config;

import com.ecommerce.vuelos.entity.Aeropuerto;
import com.ecommerce.vuelos.entity.Carrito;
import com.ecommerce.vuelos.entity.Categoria;
import com.ecommerce.vuelos.entity.Clase;
import com.ecommerce.vuelos.entity.Descuento;
import com.ecommerce.vuelos.entity.Disponibilidad;
import com.ecommerce.vuelos.entity.EstadoVuelo;
import com.ecommerce.vuelos.entity.Foto;
import com.ecommerce.vuelos.entity.Rol;
import com.ecommerce.vuelos.entity.TipoDescuento;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.repository.AeropuertoRepository;
import com.ecommerce.vuelos.repository.CategoriaRepository;
import com.ecommerce.vuelos.repository.ClaseRepository;
import com.ecommerce.vuelos.repository.DescuentoRepository;
import com.ecommerce.vuelos.repository.DisponibilidadRepository;
import com.ecommerce.vuelos.repository.FotoRepository;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AeropuertoRepository aeropuertoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private VueloRepository vueloRepository;
    @Autowired
    private ClaseRepository claseRepository;
    @Autowired
    private DisponibilidadRepository disponibilidadRepository;
    @Autowired
    private DescuentoRepository descuentoRepository;
    @Autowired
    private FotoRepository fotoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Se agrega cada aeropuerto que falte (no solo con la base vacia), asi una
        // base que ya existia tambien recibe los destinos nuevos.
        List.of(
                aeropuerto("EZE", "Ministro Pistarini", "Buenos Aires", "Buenos Aires", "Argentina"),
                aeropuerto("AEP", "Jorge Newbery", "Buenos Aires", "Buenos Aires", "Argentina"),
                aeropuerto("COR", "Ingeniero Taravella", "Córdoba", "Córdoba", "Argentina"),
                aeropuerto("MDZ", "El Plumerillo", "Mendoza", "Mendoza", "Argentina"),
                aeropuerto("BRC", "Teniente Luis Candelaria", "Bariloche", "Río Negro", "Argentina"),
                aeropuerto("IGR", "Cataratas del Iguazú", "Puerto Iguazú", "Misiones", "Argentina"),
                aeropuerto("USH", "Malvinas Argentinas", "Ushuaia", "Tierra del Fuego", "Argentina"),
                aeropuerto("FTE", "Comandante Armando Tola", "El Calafate", "Santa Cruz", "Argentina"),
                aeropuerto("SLA", "Martín Miguel de Güemes", "Salta", "Salta", "Argentina"),
                aeropuerto("MDQ", "Astor Piazzolla", "Mar del Plata", "Buenos Aires", "Argentina"),
                aeropuerto("GIG", "Galeão", "Río de Janeiro", "Río de Janeiro", "Brasil", "America/Sao_Paulo"),
                aeropuerto("SCL", "Arturo Merino Benítez", "Santiago de Chile", "Región Metropolitana", "Chile",
                        "America/Santiago"),
                aeropuerto("CUN", "Internacional de Cancún", "Cancún", "Quintana Roo", "México", "America/Cancun"),
                aeropuerto("PUJ", "Internacional de Punta Cana", "Punta Cana", "La Altagracia",
                        "República Dominicana", "America/Santo_Domingo"),
                aeropuerto("MIA", "Miami International", "Miami", "Florida", "Estados Unidos", "America/New_York"),
                aeropuerto("JFK", "John F. Kennedy", "Nueva York", "Nueva York", "Estados Unidos", "America/New_York"),
                aeropuerto("MAD", "Barajas", "Madrid", "Madrid", "España", "Europe/Madrid"),
                aeropuerto("BCN", "El Prat", "Barcelona", "Cataluña", "España", "Europe/Madrid"),
                aeropuerto("FCO", "Fiumicino", "Roma", "Lacio", "Italia", "Europe/Rome"))
                .forEach(aeropuerto -> {
                    if (!aeropuertoRepository.existsById(aeropuerto.getCodigoIata())) {
                        aeropuertoRepository.save(aeropuerto);
                    }
                });

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

        cargarVuelosDeDemostracion();
    }

    /**
     * Un vuelo de la demo: ruta, horario, precios por clase (en pesos) y su foto.
     * Precio de Primera en 0 = el vuelo no tiene esa clase.
     */
    private record VueloDemo(String numero, String origen, String destino, String categoria,
                             int diasDesdeHoy, LocalTime hora, int duracionMinutos,
                             int economica, int ejecutiva, int primera,
                             int porcentajeDescuento, String foto, String descripcion) {
    }

    private static final List<VueloDemo> VUELOS_DEMO = List.of(
            // Cabotaje: salen de Aeroparque
            new VueloDemo("BC1402", "AEP", "BRC", "Cabotaje", 18, LocalTime.of(7, 15), 140,
                    145_900, 349_000, 0, 15, "bariloche.jpg",
                    "Vuelo directo a Bariloche. Lagos, bosques y centros de ski en la Patagonia andina."),
            new VueloDemo("BC1736", "AEP", "IGR", "Cabotaje", 12, LocalTime.of(9, 40), 115,
                    128_400, 298_000, 0, 20, "iguazu.jpg",
                    "Directo a Puerto Iguazú para conocer las Cataratas, una de las maravillas naturales del mundo."),
            new VueloDemo("BC1880", "AEP", "USH", "Cabotaje", 25, LocalTime.of(6, 30), 215,
                    189_700, 432_000, 0, 0, "ushuaia.jpg",
                    "Viajá al fin del mundo: Canal Beagle, Parque Nacional Tierra del Fuego y glaciares."),
            new VueloDemo("BC1866", "AEP", "FTE", "Cabotaje", 31, LocalTime.of(8, 5), 200,
                    176_200, 405_000, 0, 0, "calafate.jpg",
                    "Directo a El Calafate, la puerta de entrada al glaciar Perito Moreno."),
            new VueloDemo("BC1450", "AEP", "SLA", "Cabotaje", 15, LocalTime.of(11, 20), 130,
                    118_900, 276_000, 0, 10, "salta.jpg",
                    "Salta la Linda: cerros de colores, quebradas y la mejor gastronomía del norte."),
            new VueloDemo("BC1510", "AEP", "MDZ", "Cabotaje", 9, LocalTime.of(14, 50), 115,
                    109_300, 254_000, 0, 0, "mendoza.jpg",
                    "Mendoza al pie de la cordillera: bodegas, viñedos y el Aconcagua."),
            new VueloDemo("BC1320", "AEP", "COR", "Cabotaje", 7, LocalTime.of(18, 10), 80,
                    86_500, 198_000, 0, 0, "cordoba.jpg",
                    "Vuelo corto a Córdoba, ideal para una escapada a las sierras."),
            new VueloDemo("BC1210", "AEP", "MDQ", "Cabotaje", 20, LocalTime.of(10, 0), 55,
                    74_800, 172_000, 0, 10, "mardelplata.jpg",
                    "La Feliz en menos de una hora: playas, rambla y el puerto de Mar del Plata."),

            // Regionales (America): salen de Ezeiza
            new VueloDemo("BC2210", "EZE", "GIG", "Regional", 22, LocalTime.of(8, 45), 180,
                    389_000, 912_000, 0, 0, "rio.jpg",
                    "Directo a Río de Janeiro: Pan de Azúcar, Copacabana e Ipanema."),
            new VueloDemo("BC2104", "EZE", "SCL", "Regional", 14, LocalTime.of(13, 30), 130,
                    298_500, 689_000, 0, 0, "santiago.jpg",
                    "Santiago de Chile, con la cordillera de los Andes como telón de fondo."),
            new VueloDemo("BC2602", "EZE", "CUN", "Regional", 40, LocalTime.of(23, 55), 540,
                    1_180_000, 2_850_000, 0, 15, "cancun.jpg",
                    "Caribe mexicano: playas de arena blanca y mar turquesa en Cancún."),
            new VueloDemo("BC2640", "EZE", "PUJ", "Regional", 45, LocalTime.of(22, 40), 520,
                    1_095_000, 2_690_000, 0, 0, "puntacana.jpg",
                    "Punta Cana, en República Dominicana: palmeras, resorts y aguas cálidas."),
            new VueloDemo("BC2800", "EZE", "MIA", "Regional", 28, LocalTime.of(23, 10), 540,
                    1_290_000, 3_150_000, 0, 0, "miami.jpg",
                    "Vuelo nocturno directo a Miami: playas, compras y conexiones a todo Estados Unidos."),
            new VueloDemo("BC2900", "EZE", "JFK", "Regional", 52, LocalTime.of(22, 15), 660,
                    1_420_000, 3_480_000, 6_900_000, 0, "nuevayork.jpg",
                    "Directo a Nueva York: Manhattan, Central Park y Broadway."),

            // Internacionales (intercontinentales): salen de Ezeiza
            new VueloDemo("BC3130", "EZE", "MAD", "Internacional", 35, LocalTime.of(21, 30), 760,
                    1_690_000, 4_250_000, 7_900_000, 12, "madrid.jpg",
                    "Vuelo directo a Madrid, la mejor puerta de entrada a Europa."),
            new VueloDemo("BC3150", "EZE", "BCN", "Internacional", 60, LocalTime.of(20, 45), 775,
                    1_740_000, 4_390_000, 0, 0, "barcelona.jpg",
                    "Barcelona: la Sagrada Familia, el Park Güell y el Mediterráneo."),
            new VueloDemo("BC3310", "EZE", "FCO", "Internacional", 70, LocalTime.of(19, 55), 800,
                    1_850_000, 4_620_000, 8_400_000, 0, "roma.jpg",
                    "Directo a Roma: el Coliseo, el Vaticano y la mejor cocina italiana."));

    /**
     * Carga cada vuelo de la demo que todavia no exista (se busca por numero de
     * vuelo). Asi funciona igual en una base nueva que en una que ya tenia datos,
     * y si un vuelo se da de baja no se vuelve a crear.
     */
    private void cargarVuelosDeDemostracion() {
        Usuario vendedor = usuarioRepository.findByUsername("vendedor").orElse(null);
        if (vendedor == null) {
            return;
        }

        for (VueloDemo demo : VUELOS_DEMO) {
            if (vueloRepository.existsByNumeroVuelo(demo.numero())) {
                continue;
            }

            LocalDateTime salida = LocalDate.now().plusDays(demo.diasDesdeHoy()).atTime(demo.hora());
            Vuelo vuelo = vueloRepository.save(Vuelo.builder()
                    .vendedor(vendedor)
                    .categoria(categoriaRepository.findByNombre(demo.categoria()).get(0))
                    .origen(aeropuertoRepository.findById(demo.origen()).orElseThrow())
                    .destino(aeropuertoRepository.findById(demo.destino()).orElseThrow())
                    .numeroVuelo(demo.numero())
                    .descripcion(demo.descripcion())
                    .fechaSalida(salida)
                    .fechaLlegada(salida.plusMinutes(demo.duracionMinutos()))
                    .precio(BigDecimal.valueOf(demo.economica()))
                    .estado(EstadoVuelo.ACTIVO)
                    .fechaAlta(LocalDateTime.now())
                    .build());

            disponibilidadRepository.save(cupo(vuelo, "Economica", 156, demo.economica()));
            disponibilidadRepository.save(cupo(vuelo, "Ejecutiva", 24, demo.ejecutiva()));
            if (demo.primera() > 0) {
                disponibilidadRepository.save(cupo(vuelo, "Primera", 8, demo.primera()));
            }

            if (demo.porcentajeDescuento() > 0) {
                descuentoRepository.save(Descuento.builder()
                        .vuelo(vuelo)
                        .tipoDescuento(TipoDescuento.PORCENTAJE)
                        .valor(BigDecimal.valueOf(demo.porcentajeDescuento()))
                        .fechaDesde(LocalDate.now().minusDays(1))
                        .fechaHasta(LocalDate.now().plusMonths(2))
                        .activo(true)
                        .build());
            }

            cargarFoto(vuelo, demo.foto());
        }
    }

    /** Lee la imagen de src/main/resources/fotos-ejemplo y la guarda como foto del vuelo. */
    private void cargarFoto(Vuelo vuelo, String archivo) {
        try (InputStream imagen = new ClassPathResource("fotos-ejemplo/" + archivo).getInputStream()) {
            fotoRepository.save(Foto.builder()
                    .vuelo(vuelo)
                    .nombreArchivo(archivo)
                    .datos(imagen.readAllBytes())
                    .orden(1)
                    .build());
        } catch (IOException e) {
            // Sin la foto el vuelo se muestra igual (con el fondo azul del destino)
        }
    }

    private Clase clase(String nombre, String descripcion, boolean equipajeBodega) {
        return Clase.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .equipajeBodega(equipajeBodega)
                .build();
    }

    private Disponibilidad cupo(Vuelo vuelo, String nombreClase, int asientos, int precio) {
        return Disponibilidad.builder()
                .vuelo(vuelo)
                .clase(claseRepository.findByNombre(nombreClase).orElseThrow())
                .asientosTotales(asientos)
                .asientosDisponibles(asientos)
                .precio(BigDecimal.valueOf(precio))
                .build();
    }

    private Aeropuerto aeropuerto(String iata, String nombre, String ciudad, String provincia, String pais) {
        return aeropuerto(iata, nombre, ciudad, provincia, pais, "America/Argentina/Buenos_Aires");
    }

    private Aeropuerto aeropuerto(String iata, String nombre, String ciudad, String provincia, String pais,
                                  String zonaHoraria) {
        return Aeropuerto.builder()
                .codigoIata(iata)
                .nombre(nombre)
                .ciudad(ciudad)
                .provincia(provincia)
                .pais(pais)
                .zonaHoraria(zonaHoraria)
                .build();
    }

    private Categoria categoria(String nombre, String descripcion) {
        return Categoria.builder().nombre(nombre).descripcion(descripcion).build();
    }

    private Usuario usuario(String username, String mail, String nombre, String apellido, Rol rol) {
        Usuario usuario = Usuario.builder()
                .username(username)
                .mail(mail)
                .password(passwordEncoder.encode(username + "123"))
                .nombre(nombre)
                .apellido(apellido)
                .rol(rol)
                .fechaRegistro(LocalDateTime.now())
                .build();


        if (rol == Rol.COMPRADOR) {
            usuario.setCarrito(Carrito.builder().usuario(usuario).build());
        }

        return usuario;
    }
}
