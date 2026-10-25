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
import java.time.format.DateTimeFormatter;
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
        // Los aeropuertos se guardan siempre: el codigo IATA es la clave, asi que los
        // que faltan se agregan y los que ya estaban se actualizan (nombres con tilde, etc.).
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
                .forEach(aeropuertoRepository::save);

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
        // Cuantas veces suma millas cada clase (tambien para las bases que ya tenian las clases)
        cargarMultiplicadorMillas("Economica", 1);
        cargarMultiplicadorMillas("Ejecutiva", 2);
        cargarMultiplicadorMillas("Primera", 3);
        // Cuantos asientos tiene cada fila en el mapa del avion
        cargarAsientosPorFila("Economica", 6);
        cargarAsientosPorFila("Ejecutiva", 4);
        cargarAsientosPorFila("Primera", 2);
        // Cuantos asientos puede tener cada clase en un vuelo (parecido a un Airbus A330)
        cargarMaximoAsientos("Economica", 250);
        cargarMaximoAsientos("Ejecutiva", 40);
        cargarMaximoAsientos("Primera", 16);

        if (usuarioRepository.count() == 0) {
            // La aerolinea tiene un unico vendedor: el administrador publica y gestiona los vuelos.
            usuarioRepository.save(usuario("admin", "admin@vuelos.com", "Admin", "Sistema", Rol.ADMIN));
            usuarioRepository.save(usuario("comprador", "comprador@vuelos.com", "Caro", "Compradora", Rol.COMPRADOR));
        }

        // El comprador de la demo arranca con millas para poder probar el canje
        // (solo la primera vez: cuando su saldo todavia no existe en la base)
        if (usuarioRepository.existsByUsernameAndMillasIsNull("comprador")) {
            Usuario comprador = usuarioRepository.findByUsername("comprador").orElseThrow();
            comprador.setMillas(MILLAS_DEMO);
            usuarioRepository.save(comprador);
        }

        cargarVuelosDeDemostracion();
        cargarVuelosDiarios();
    }

    private static final int MILLAS_DEMO = 150_000;

    private void cargarMaximoAsientos(String nombreClase, int maximoAsientos) {
        claseRepository.findByNombre(nombreClase).ifPresent(clase -> {
            if (clase.getMaximoAsientos() == null) {
                clase.setMaximoAsientos(maximoAsientos);
                claseRepository.save(clase);
            }
        });
    }

    private void cargarAsientosPorFila(String nombreClase, int asientosPorFila) {
        claseRepository.findByNombre(nombreClase).ifPresent(clase -> {
            if (clase.getAsientosPorFila() == null) {
                clase.setAsientosPorFila(asientosPorFila);
                claseRepository.save(clase);
            }
        });
    }

    /** Solo lo carga si la clase todavia no tiene el dato, asi no pisa un valor cambiado en la base. */
    private void cargarMultiplicadorMillas(String nombreClase, int multiplicador) {
        claseRepository.findByNombre(nombreClase).ifPresent(clase -> {
            if (clase.getMultiplicadorMillas() == null) {
                clase.setMultiplicadorMillas(multiplicador);
                claseRepository.save(clase);
            }
        });
    }

    /**
     * Una ruta de la demo con su vuelo de ida (desde Buenos Aires) y los datos para
     * armar los de vuelta. Precio de Primera en 0 = el vuelo no tiene esa clase.
     */
    private record RutaDemo(int numeroBase, String origen, String destino, String categoria,
                            int diasDesdeHoy, LocalTime hora, int duracionMinutos,
                            int economica, int ejecutiva, int primera,
                            int porcentajeDescuento, String foto, String ciudad, String descripcion) {
    }

    private static final List<RutaDemo> RUTAS_DEMO = List.of(
            // Cabotaje: salen de Aeroparque
            new RutaDemo(1402, "AEP", "BRC", "Cabotaje", 18, LocalTime.of(7, 15), 140,
                    145_900, 349_000, 0, 15, "bariloche.jpg", "Bariloche",
                    "Vuelo directo a Bariloche. Lagos, bosques y centros de ski en la Patagonia andina."),
            new RutaDemo(1736, "AEP", "IGR", "Cabotaje", 12, LocalTime.of(9, 40), 115,
                    128_400, 298_000, 0, 20, "iguazu.jpg", "Puerto Iguazú",
                    "Directo a Puerto Iguazú para conocer las Cataratas, una de las maravillas naturales del mundo."),
            new RutaDemo(1880, "AEP", "USH", "Cabotaje", 25, LocalTime.of(6, 30), 215,
                    189_700, 432_000, 0, 0, "ushuaia.jpg", "Ushuaia",
                    "Viajá al fin del mundo: Canal Beagle, Parque Nacional Tierra del Fuego y glaciares."),
            new RutaDemo(1866, "AEP", "FTE", "Cabotaje", 31, LocalTime.of(8, 5), 200,
                    176_200, 405_000, 0, 0, "calafate.jpg", "El Calafate",
                    "Directo a El Calafate, la puerta de entrada al glaciar Perito Moreno."),
            new RutaDemo(1450, "AEP", "SLA", "Cabotaje", 15, LocalTime.of(11, 20), 130,
                    118_900, 276_000, 0, 10, "salta.jpg", "Salta",
                    "Salta la Linda: cerros de colores, quebradas y la mejor gastronomía del norte."),
            new RutaDemo(1510, "AEP", "MDZ", "Cabotaje", 9, LocalTime.of(14, 50), 115,
                    109_300, 254_000, 0, 0, "mendoza.jpg", "Mendoza",
                    "Mendoza al pie de la cordillera: bodegas, viñedos y el Aconcagua."),
            new RutaDemo(1320, "AEP", "COR", "Cabotaje", 7, LocalTime.of(18, 10), 80,
                    86_500, 198_000, 0, 0, "cordoba.jpg", "Córdoba",
                    "Vuelo corto a Córdoba, ideal para una escapada a las sierras."),
            new RutaDemo(1210, "AEP", "MDQ", "Cabotaje", 20, LocalTime.of(10, 0), 55,
                    74_800, 172_000, 0, 10, "mardelplata.jpg", "Mar del Plata",
                    "La Feliz en menos de una hora: playas, rambla y el puerto de Mar del Plata."),

            // Regionales (America): salen de Ezeiza
            new RutaDemo(2210, "EZE", "GIG", "Regional", 22, LocalTime.of(8, 45), 180,
                    389_000, 912_000, 0, 0, "rio.jpg", "Río de Janeiro",
                    "Directo a Río de Janeiro: Pan de Azúcar, Copacabana e Ipanema."),
            new RutaDemo(2104, "EZE", "SCL", "Regional", 14, LocalTime.of(13, 30), 130,
                    298_500, 689_000, 0, 0, "santiago.jpg", "Santiago de Chile",
                    "Santiago de Chile, con la cordillera de los Andes como telón de fondo."),
            new RutaDemo(2602, "EZE", "CUN", "Regional", 40, LocalTime.of(23, 55), 540,
                    1_180_000, 2_850_000, 5_600_000, 15, "cancun.jpg", "Cancún",
                    "Caribe mexicano: playas de arena blanca y mar turquesa en Cancún."),
            new RutaDemo(2640, "EZE", "PUJ", "Regional", 45, LocalTime.of(22, 40), 520,
                    1_095_000, 2_690_000, 5_300_000, 0, "puntacana.jpg", "Punta Cana",
                    "Punta Cana, en República Dominicana: palmeras, resorts y aguas cálidas."),
            new RutaDemo(2800, "EZE", "MIA", "Regional", 28, LocalTime.of(23, 10), 540,
                    1_290_000, 3_150_000, 6_200_000, 0, "miami.jpg", "Miami",
                    "Vuelo nocturno directo a Miami: playas, compras y conexiones a todo Estados Unidos."),
            new RutaDemo(2900, "EZE", "JFK", "Regional", 52, LocalTime.of(22, 15), 660,
                    1_420_000, 3_480_000, 6_900_000, 0, "nuevayork.jpg", "Nueva York",
                    "Directo a Nueva York: Manhattan, Central Park y Broadway."),

            // Internacionales (intercontinentales): salen de Ezeiza
            new RutaDemo(3130, "EZE", "MAD", "Internacional", 35, LocalTime.of(21, 30), 760,
                    1_690_000, 4_250_000, 7_900_000, 12, "madrid.jpg", "Madrid",
                    "Vuelo directo a Madrid, la mejor puerta de entrada a Europa."),
            new RutaDemo(3150, "EZE", "BCN", "Internacional", 60, LocalTime.of(20, 45), 775,
                    1_740_000, 4_390_000, 8_100_000, 0, "barcelona.jpg", "Barcelona",
                    "Barcelona: la Sagrada Familia, el Park Güell y el Mediterráneo."),
            new RutaDemo(3310, "EZE", "FCO", "Internacional", 70, LocalTime.of(19, 55), 800,
                    1_850_000, 4_620_000, 8_400_000, 0, "roma.jpg", "Roma",
                    "Directo a Roma: el Coliseo, el Vaticano y la mejor cocina italiana."));

    /** Vuelos por sentido en cada ruta (en distintos dias y horarios). */
    private static final int FRECUENCIAS = 3;
    /** Variacion de precio de cada frecuencia respecto del precio base, en porcentaje. */
    private static final int[] AJUSTE_PRECIO = {100, 108, 94};
    /** Dias que el pasajero se queda en el destino antes de volver. */
    private static final int DIAS_DE_ESTADIA = 6;

    /** Cuantos dias hacia adelante se generan vuelos todos los dias. */
    private static final int DIAS_CON_VUELOS = 120;
    /** Solo 1 de cada N dias lleva foto (las fotos pesan en la base). Con 1, todos tienen foto. */
    private static final int FOTO_CADA_N_DIAS = 7;
    /** Minutos que se corre el horario base de la ruta segun el dia, para que no salgan todos a la misma hora. */
    private static final int[] VARIACION_HORARIO = {0, 150, -120, 300, 60, -60, 210, -180, 120};

    /**
     * Por cada ruta arma 3 vuelos de ida y 3 de vuelta. Los numeros siguen la
     * costumbre de las aerolineas: par para la ida (BC1402, BC1404, BC1406) e
     * impar para la vuelta (BC1403, BC1405, BC1407).
     *
     * Solo se crea cada vuelo que todavia no exista (se busca por numero), asi
     * funciona igual en una base nueva que en una que ya tenia datos, y si un
     * vuelo se da de baja no se vuelve a crear.
     */
    private void cargarVuelosDeDemostracion() {
        Usuario admin = usuarioRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            return;
        }

        for (RutaDemo ruta : RUTAS_DEMO) {
            for (int i = 0; i < FRECUENCIAS; i++) {
                int dias = ruta.diasDesdeHoy() + i * 5;
                int ajuste = AJUSTE_PRECIO[i];

                // Ida: desde Buenos Aires al destino, con la promo de la ruta si tiene
                LocalTime horaIda = ruta.hora().plusMinutes(i * 335L);
                crearVuelo(admin, "BC" + (ruta.numeroBase() + 2 * i), ruta.origen(), ruta.destino(),
                        ruta, LocalDate.now().plusDays(dias).atTime(horaIda), ajuste,
                        ruta.porcentajeDescuento(), ruta.foto(), ruta.descripcion(), true);

                // Vuelta: del destino a Buenos Aires, unos dias despues
                LocalTime horaVuelta = ruta.hora().plusMinutes(540 + i * 255L);
                crearVuelo(admin, "BC" + (ruta.numeroBase() + 2 * i + 1), ruta.destino(), ruta.origen(),
                        ruta, LocalDate.now().plusDays(dias + DIAS_DE_ESTADIA).atTime(horaVuelta), ajuste,
                        0, "buenosaires.jpg", "Regreso directo desde " + ruta.ciudad() + " a Buenos Aires.",
                        true);
            }
        }
    }

    /**
     * Crea un vuelo de ida y uno de vuelta por dia y por ruta, asi cualquier fecha
     * que se busque tiene al menos un vuelo. El numero lleva la fecha (ddMM) para
     * ser unico y para no volver a crear lo que ya existe.
     */
    private void cargarVuelosDiarios() {
        Usuario admin = usuarioRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            return;
        }
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("ddMM");
        LocalDateTime ahora = LocalDateTime.now();

        for (int r = 0; r < RUTAS_DEMO.size(); r++) {
            RutaDemo ruta = RUTAS_DEMO.get(r);
            for (int dia = 0; dia < DIAS_CON_VUELOS; dia++) {
                LocalDate fecha = LocalDate.now().plusDays(dia);
                String sufijo = fecha.format(formato);
                boolean conFoto = dia % FOTO_CADA_N_DIAS == 0;
                // El horario cambia de un dia a otro (y distinto para ida y vuelta)
                int variacionIda = VARIACION_HORARIO[(dia + r) % VARIACION_HORARIO.length];
                int variacionVuelta = VARIACION_HORARIO[(dia + r + 4) % VARIACION_HORARIO.length];

                LocalDateTime salidaIda = fecha.atTime(ruta.hora().plusMinutes(90L + variacionIda));
                if (salidaIda.isAfter(ahora)) {
                    crearVuelo(admin, "BC" + ruta.numeroBase() + sufijo, ruta.origen(), ruta.destino(),
                            ruta, salidaIda, 100, 0, conFoto ? ruta.foto() : null,
                            ruta.descripcion(), false);
                }

                LocalDateTime salidaVuelta = fecha.atTime(ruta.hora().plusMinutes(630L + variacionVuelta));
                if (salidaVuelta.isAfter(ahora)) {
                    crearVuelo(admin, "BC" + (ruta.numeroBase() + 1) + sufijo, ruta.destino(), ruta.origen(),
                            ruta, salidaVuelta, 100, 0, conFoto ? "buenosaires.jpg" : null,
                            "Regreso directo desde " + ruta.ciudad() + " a Buenos Aires.", false);
                }
            }
        }
    }

    private void crearVuelo(Usuario admin, String numero, String origen, String destino, RutaDemo ruta,
                            LocalDateTime salida, int ajustePrecio, int porcentajeDescuento,
                            String foto, String descripcion, boolean renovar) {
        Vuelo existente = vueloRepository.findFirstByNumeroVuelo(numero).orElse(null);
        if (existente != null) {
            if (renovar) {
                // El vuelo ya estaba: se le suma Primera si la ruta ahora la tiene, y si ya
                // salio se le renuevan las fechas para que la demo siempre tenga vuelos futuros
                agregarPrimeraSiFalta(existente, ruta, ajustePrecio);
                renovarSiYaSalio(existente, salida, ruta);
                renovarPromocionVencida(existente);
            } else {
                // Vuelo diario ya creado: solo se le actualiza el horario si cambio
                ajustarHorario(existente, salida, ruta);
            }
            return;
        }

        int economica = ajustar(ruta.economica(), ajustePrecio);
        Vuelo vuelo = vueloRepository.save(Vuelo.builder()
                .vendedor(admin)
                .categoria(categoriaRepository.findByNombre(ruta.categoria()).get(0))
                .origen(aeropuertoRepository.findById(origen).orElseThrow())
                .destino(aeropuertoRepository.findById(destino).orElseThrow())
                .numeroVuelo(numero)
                .descripcion(descripcion)
                .fechaSalida(salida)
                .fechaLlegada(salida.plusMinutes(ruta.duracionMinutos()))
                .precio(BigDecimal.valueOf(economica))
                .estado(EstadoVuelo.ACTIVO)
                .fechaAlta(LocalDateTime.now())
                .build());

        disponibilidadRepository.save(cupo(vuelo, "Economica", 156, economica));
        disponibilidadRepository.save(cupo(vuelo, "Ejecutiva", 24, ajustar(ruta.ejecutiva(), ajustePrecio)));
        if (ruta.primera() > 0) {
            disponibilidadRepository.save(cupo(vuelo, "Primera", 8, ajustar(ruta.primera(), ajustePrecio)));
        }

        if (porcentajeDescuento > 0) {
            descuentoRepository.save(Descuento.builder()
                    .vuelo(vuelo)
                    .tipoDescuento(TipoDescuento.PORCENTAJE)
                    .valor(BigDecimal.valueOf(porcentajeDescuento))
                    .fechaDesde(LocalDate.now().minusDays(1))
                    .fechaHasta(finDePromocion(salida))
                    .activo(true)
                    .build());
        }

        if (foto != null) {
            cargarFoto(vuelo, foto);
        }
    }

    /**
     * Para las bases que ya tenian los vuelos de demo: si la ruta ofrece Primera
     * y el vuelo todavia no tiene ese cupo, se lo agrega.
     */
    private void agregarPrimeraSiFalta(Vuelo vuelo, RutaDemo ruta, int ajustePrecio) {
        if (ruta.primera() == 0) {
            return;
        }
        Long primeraId = claseRepository.findByNombre("Primera").orElseThrow().getId();
        if (!disponibilidadRepository.existsByVueloIdAndClaseId(vuelo.getId(), primeraId)) {
            disponibilidadRepository.save(cupo(vuelo, "Primera", 8, ajustar(ruta.primera(), ajustePrecio)));
        }
    }

    /**
     * Los vuelos de la demo tienen fechas fijas desde que se cargaron por primera vez,
     * asi que con los dias van saliendo. Si uno ya salio, se lo vuelve a programar en
     * la misma fecha relativa a hoy que tendria en una base nueva. Los que el admin
     * dio de baja (ELIMINADO) no se tocan.
     */
    private void renovarSiYaSalio(Vuelo vuelo, LocalDateTime nuevaSalida, RutaDemo ruta) {
        boolean yaSalio = vuelo.getFechaSalida().isBefore(LocalDateTime.now());
        if (!yaSalio || vuelo.getEstado() == EstadoVuelo.ELIMINADO) {
            return;
        }
        vuelo.setFechaSalida(nuevaSalida);
        vuelo.setFechaLlegada(nuevaSalida.plusMinutes(ruta.duracionMinutos()));
        vueloRepository.save(vuelo);
    }

    /** Reprograma el horario de un vuelo diario que todavia no salio. Los eliminados no se tocan. */
    private void ajustarHorario(Vuelo vuelo, LocalDateTime salida, RutaDemo ruta) {
        if (vuelo.getEstado() == EstadoVuelo.ELIMINADO
                || vuelo.getFechaSalida().isBefore(LocalDateTime.now())
                || salida.equals(vuelo.getFechaSalida())) {
            return;
        }
        vuelo.setFechaSalida(salida);
        vuelo.setFechaLlegada(salida.plusMinutes(ruta.duracionMinutos()));
        vueloRepository.save(vuelo);
    }

    /**
     * Si la promocion de la demo vencio y sigue activa, se la renueva desde hoy.
     * Tambien se corrigen las que seguian despues de la salida del vuelo.
     */
    private void renovarPromocionVencida(Vuelo vuelo) {
        LocalDate fin = finDePromocion(vuelo.getFechaSalida());
        for (Descuento descuento : descuentoRepository.findByVueloIdAndActivoTrue(vuelo.getId())) {
            if (descuento.getFechaHasta().isBefore(LocalDate.now())) {
                descuento.setFechaDesde(LocalDate.now().minusDays(1));
                descuento.setFechaHasta(fin);
                descuentoRepository.save(descuento);
            } else if (descuento.getFechaHasta().isAfter(fin)) {
                descuento.setFechaHasta(fin);
                descuentoRepository.save(descuento);
            }
        }
    }

    /** Una promo dura dos meses, pero como tarde hasta el dia que sale el vuelo. */
    private LocalDate finDePromocion(LocalDateTime salida) {
        LocalDate dosMeses = LocalDate.now().plusMonths(2);
        return salida.toLocalDate().isBefore(dosMeses) ? salida.toLocalDate() : dosMeses;
    }

    /** Aplica el porcentaje y redondea a centenas, como un precio de verdad. */
    private int ajustar(int precio, int porcentaje) {
        return Math.round(precio * porcentaje / 10_000f) * 100;
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