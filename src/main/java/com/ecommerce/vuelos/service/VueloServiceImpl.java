package com.ecommerce.vuelos.service;

import com.ecommerce.vuelos.dto.PaginaResponse;
import com.ecommerce.vuelos.dto.vuelo.EstadoVueloResponse;
import com.ecommerce.vuelos.dto.vuelo.ResumenPanelResponse;
import com.ecommerce.vuelos.dto.vuelo.VueloRequest;
import com.ecommerce.vuelos.dto.vuelo.VueloResponse;
import com.ecommerce.vuelos.entity.Aeropuerto;
import com.ecommerce.vuelos.entity.Categoria;
import com.ecommerce.vuelos.entity.EstadoVuelo;
import com.ecommerce.vuelos.entity.Usuario;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.event.VueloModificadoEvent;
import com.ecommerce.vuelos.exception.BadRequestException;
import com.ecommerce.vuelos.exception.ResourceNotFoundException;
import com.ecommerce.vuelos.repository.AeropuertoRepository;
import com.ecommerce.vuelos.repository.CategoriaRepository;
import com.ecommerce.vuelos.repository.DisponibilidadRepository;
import com.ecommerce.vuelos.repository.UsuarioRepository;
import com.ecommerce.vuelos.repository.VueloRepository;
import com.ecommerce.vuelos.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VueloServiceImpl implements VueloService {

    @Autowired
    private VueloRepository vueloRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private AeropuertoRepository aeropuertoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private DisponibilidadRepository disponibilidadRepository;
    @Autowired
    private ApplicationEventPublisher eventos;

    @Override
    public PaginaResponse<VueloResponse> buscar(String origen, String destino, Long categoriaId, Long claseId,
                                                BigDecimal precioMin, BigDecimal precioMax,
                                                LocalDate fechaDesde, LocalDate fechaHasta, Long vendedorId,
                                                String texto, PageRequest pageRequest) {
        // Cada Specification devuelve null si su filtro vino vacio, asi que las
        // combinamos todas y quedan solo las que el cliente realmente mando.
        Specification<Vuelo> spec = Specification.allOf(
                VueloSpecifications.soloPublicados(),
                VueloSpecifications.todaviaNoSalio(),
                VueloSpecifications.origenContiene(origen),
                VueloSpecifications.destinoContiene(destino),
                VueloSpecifications.deCategoria(categoriaId),
                VueloSpecifications.deClase(claseId),
                VueloSpecifications.precioMinimo(precioMin),
                VueloSpecifications.precioMaximo(precioMax),
                VueloSpecifications.saleDesde(fechaDesde),
                VueloSpecifications.saleHasta(fechaHasta),
                VueloSpecifications.delVendedor(vendedorId),
                VueloSpecifications.textoContiene(texto));

        boolean hayFiltros = origen != null || destino != null || categoriaId != null || claseId != null
                || precioMin != null || precioMax != null || fechaDesde != null || fechaHasta != null
                || vendedorId != null || (texto != null && !texto.isBlank());
        String mensajeSiVacia = hayFiltros
                ? "No hay vuelos que coincidan con los filtros indicados"
                : "No hay vuelos disponibles por el momento";

        return PaginaResponse.desde(vueloRepository.findAll(spec, pageRequest).map(VueloResponse::desde), mensajeSiVacia);
    }

    @Override
    public ResumenPanelResponse resumen() {
        LocalDateTime ahora = LocalDateTime.now();
        long vuelos = vueloRepository.count(Specification.allOf(
                VueloSpecifications.soloPublicados(), VueloSpecifications.todaviaNoSalio()));
        return new ResumenPanelResponse(vuelos,
                disponibilidadRepository.sumarAsientosVendidos(ahora),
                disponibilidadRepository.sumarAsientosTotales(ahora));
    }

    @Override
    public VueloResponse obtener(Long id) {
        Vuelo vuelo = buscarPorId(id);
        // Un vuelo dado de baja ya no se muestra (las compras que lo tienen lo siguen viendo en su detalle)
        if (vuelo.getEstado() == EstadoVuelo.ELIMINADO) {
            throw new ResourceNotFoundException("Vuelo no encontrado: " + id);
        }
        return VueloResponse.desde(vuelo);
    }

    @Override
    @Transactional
    public VueloResponse crear(VueloRequest request, Long vendedorId) {
        Usuario vendedor = usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + vendedorId));

        Vuelo vuelo = Vuelo.builder()
                .vendedor(vendedor)
                .categoria(buscarCategoria(request.getCategoriaId()))
                .origen(buscarAeropuerto(request.getOrigenIata()))
                .destino(buscarAeropuerto(request.getDestinoIata()))
                .numeroVuelo(request.getNumeroVuelo())
                .descripcion(request.getDescripcion())
                .fechaSalida(request.getFechaSalida())
                .fechaLlegada(request.getFechaLlegada())
                // Sin clases todavia no tiene precio: se completa al cargar la primera clase
                .precio(request.getPrecio() != null ? request.getPrecio() : BigDecimal.ZERO)
                .estado(EstadoVuelo.ACTIVO)
                .fechaAlta(LocalDateTime.now())
                .build();

        validarFechas(vuelo);
        validarNumeroUnico(vuelo);
        return VueloResponse.desde(vueloRepository.save(vuelo));
    }

    @Override
    @Transactional
    public VueloResponse actualizar(Long id, VueloRequest request, UsuarioPrincipal principal) {
        Vuelo vuelo = buscarPorId(id);
        validarPropiedad(vuelo, principal);

        // Al que ya compro no se le puede cambiar adonde viaja: con pasajes vendidos la ruta queda fija
        boolean cambiaRuta = !vuelo.getOrigen().getCodigoIata().equalsIgnoreCase(request.getOrigenIata())
                || !vuelo.getDestino().getCodigoIata().equalsIgnoreCase(request.getDestinoIata());
        if (cambiaRuta && !usuarioRepository.compradoresDelVuelo(id).isEmpty()) {
            throw new BadRequestException("El vuelo ya tiene pasajes vendidos: no se le puede cambiar el origen ni el destino");
        }

        vuelo.setCategoria(buscarCategoria(request.getCategoriaId()));
        vuelo.setOrigen(buscarAeropuerto(request.getOrigenIata()));
        vuelo.setDestino(buscarAeropuerto(request.getDestinoIata()));
        vuelo.setNumeroVuelo(request.getNumeroVuelo());
        vuelo.setDescripcion(request.getDescripcion());
        // Los horarios que conocian los que ya compraron, para avisarles si cambian
        LocalDateTime salidaAnterior = vuelo.getFechaSalida();
        LocalDateTime llegadaAnterior = vuelo.getFechaLlegada();
        vuelo.setFechaSalida(request.getFechaSalida());
        vuelo.setFechaLlegada(request.getFechaLlegada());
        // El precio lo manejan las clases; solo se toma si viene y el vuelo todavia no tiene clases
        if (request.getPrecio() != null && vuelo.getDisponibilidades().isEmpty()) {
            vuelo.setPrecio(request.getPrecio());
        }

        validarFechas(vuelo);
        validarNumeroUnico(vuelo);
        Vuelo guardado = vueloRepository.save(vuelo);

        boolean reprogramado = !salidaAnterior.equals(vuelo.getFechaSalida())
                || !llegadaAnterior.equals(vuelo.getFechaLlegada());
        // Un vuelo cancelado no se reprograma para nadie: a sus compradores ya se les aviso la cancelacion
        if (reprogramado && vuelo.getEstado() != EstadoVuelo.CANCELADO) {
            avisarCompradores(vuelo, VueloModificadoEvent.Motivo.REPROGRAMADO, salidaAnterior, llegadaAnterior);
        }
        return VueloResponse.desde(guardado);
    }

    @Override
    @Transactional
    public EstadoVueloResponse cambiarEstado(Long id, EstadoVuelo nuevoEstado, UsuarioPrincipal principal) {
        Vuelo vuelo = buscarPorId(id);
        validarPropiedad(vuelo, principal);

        if (vuelo.getEstado() == EstadoVuelo.ELIMINADO) {
            throw new BadRequestException("El vuelo fue eliminado y ya no se puede modificar");
        }
        if (vuelo.getEstado() == nuevoEstado) {
            throw new BadRequestException("El vuelo ya esta en estado " + nuevoEstado);
        }

        EstadoVuelo anterior = vuelo.getEstado();
        vuelo.setEstado(nuevoEstado);
        // La baja logica deja la fecha: el vuelo sigue en la base para no romper las ordenes que lo referencian.
        if (nuevoEstado == EstadoVuelo.ELIMINADO) {
            vuelo.setFechaBaja(LocalDateTime.now());
        }
        vueloRepository.save(vuelo);

        String mensaje = mensajeDeEstado(vuelo, nuevoEstado);
        VueloModificadoEvent.Motivo motivo = motivoDelAviso(anterior, nuevoEstado);
        if (motivo != null) {
            int avisados = avisarCompradores(vuelo, motivo, null, null);
            if (avisados > 0) {
                mensaje += ". Se aviso por mail a " + avisados + (avisados == 1 ? " comprador" : " compradores");
            }
        }
        return new EstadoVueloResponse(vuelo.getId(), nuevoEstado, mensaje);
    }

    /**
     * Que cambios le importan al que ya compro. Pausar solo frena la venta (el que compro vuela igual), y
     * eliminar un vuelo que ya estaba cancelado no es una novedad: en esos casos no hay aviso.
     */
    private VueloModificadoEvent.Motivo motivoDelAviso(EstadoVuelo anterior, EstadoVuelo nuevo) {
        return switch (nuevo) {
            case DEMORADO -> VueloModificadoEvent.Motivo.DEMORADO;
            case CANCELADO -> VueloModificadoEvent.Motivo.CANCELADO;
            case ELIMINADO -> anterior == EstadoVuelo.CANCELADO ? null : VueloModificadoEvent.Motivo.CANCELADO;
            case ACTIVO -> anterior == EstadoVuelo.DEMORADO || anterior == EstadoVuelo.CANCELADO
                    ? VueloModificadoEvent.Motivo.NORMALIZADO
                    : null;
            case PAUSADO -> null;
        };
    }

    /**
     * Publica el aviso para los que compraron pasajes del vuelo; el mail sale cuando se confirma la
     * transaccion. Si el vuelo ya aterrizo (con el horario que conocian) no se avisa nada.
     * Devuelve a cuantos compradores se les va a avisar.
     */
    private int avisarCompradores(Vuelo vuelo, VueloModificadoEvent.Motivo motivo,
                                  LocalDateTime salidaAnterior, LocalDateTime llegadaAnterior) {
        LocalDateTime llegadaConocida = llegadaAnterior != null ? llegadaAnterior : vuelo.getFechaLlegada();
        if (llegadaConocida.isBefore(LocalDateTime.now())) {
            return 0;
        }
        List<VueloModificadoEvent.Comprador> compradores = usuarioRepository.compradoresDelVuelo(vuelo.getId())
                .stream()
                .map((usuario) -> new VueloModificadoEvent.Comprador(usuario.getMail(), usuario.getNombre()))
                .toList();
        if (compradores.isEmpty()) {
            return 0;
        }
        eventos.publishEvent(new VueloModificadoEvent(motivo, vuelo.getNumeroVuelo(),
                vuelo.getOrigen().getCiudad(), vuelo.getDestino().getCiudad(),
                vuelo.getFechaSalida(), vuelo.getFechaLlegada(), salidaAnterior, llegadaAnterior, compradores));
        return compradores.size();
    }

    private String mensajeDeEstado(Vuelo vuelo, EstadoVuelo estado) {
        String numero = vuelo.getNumeroVuelo();
        return switch (estado) {
            case ACTIVO -> "El vuelo " + numero + " esta activo";
            case DEMORADO -> "El vuelo " + numero + " fue marcado como demorado";
            case PAUSADO -> "El vuelo " + numero + " fue pausado";
            case CANCELADO -> "El vuelo " + numero + " fue cancelado";
            case ELIMINADO -> "El vuelo " + numero + " fue eliminado correctamente";
        };
    }

    /** Solo el vendedor que publico el vuelo, o un ADMIN, pueden tocarlo. */
    private void validarPropiedad(Vuelo vuelo, UsuarioPrincipal principal) {
        if (principal.esAdmin()) {
            return;
        }
        if (!vuelo.getVendedor().getId().equals(principal.getId())) {
            throw new BadRequestException("Solo el vendedor que publico el vuelo puede modificarlo");
        }
    }

    /** Un vuelo directo dura entre 30 minutos y 20 horas (el mas largo del mundo dura unas 19). */
    private static final int DURACION_MINIMA_MINUTOS = 30;
    private static final int DURACION_MAXIMA_HORAS = 20;

    /** Que los datos del vuelo tengan logica: ruta real y horarios posibles. */
    private void validarFechas(Vuelo vuelo) {
        if (vuelo.getOrigen().getCodigoIata().equals(vuelo.getDestino().getCodigoIata())) {
            throw new BadRequestException("El origen y el destino tienen que ser distintos");
        }
        validarRuta(vuelo);
        if (!vuelo.getFechaLlegada().isAfter(vuelo.getFechaSalida())) {
            throw new BadRequestException("La fecha de llegada debe ser posterior a la de salida");
        }
        long minutos = Duration.between(vuelo.getFechaSalida(), vuelo.getFechaLlegada()).toMinutes();
        if (minutos < DURACION_MINIMA_MINUTOS) {
            throw new BadRequestException("El vuelo tiene que durar al menos " + DURACION_MINIMA_MINUTOS + " minutos");
        }
        if (minutos > DURACION_MAXIMA_HORAS * 60L) {
            throw new BadRequestException("Un vuelo directo no puede durar mas de " + DURACION_MAXIMA_HORAS
                    + " horas: revisa la fecha de llegada");
        }
    }

    /**
     * Igual que en el buscador del pasajero: todos los vuelos salen de Buenos Aires (ida)
     * o vuelven a Buenos Aires (vuelta). Los de cabotaje usan Aeroparque y los
     * internacionales Ezeiza.
     */
    private void validarRuta(Vuelo vuelo) {
        boolean saleDeBuenosAires = AEROPUERTOS_BUENOS_AIRES.contains(vuelo.getOrigen().getCodigoIata());
        boolean llegaABuenosAires = AEROPUERTOS_BUENOS_AIRES.contains(vuelo.getDestino().getCodigoIata());
        if (saleDeBuenosAires == llegaABuenosAires) {
            throw new BadRequestException(
                    "El vuelo tiene que salir de Buenos Aires o volver a Buenos Aires (Aeroparque o Ezeiza)");
        }

        String buenosAires = saleDeBuenosAires ? vuelo.getOrigen().getCodigoIata() : vuelo.getDestino().getCodigoIata();
        String paisDelOtro = saleDeBuenosAires ? vuelo.getDestino().getPais() : vuelo.getOrigen().getPais();
        boolean cabotaje = "Argentina".equals(paisDelOtro);
        if (cabotaje && !buenosAires.equals("AEP")) {
            throw new BadRequestException("Los vuelos dentro de Argentina salen y llegan a Aeroparque (AEP)");
        }
        if (!cabotaje && !buenosAires.equals("EZE")) {
            throw new BadRequestException("Los vuelos internacionales salen y llegan a Ezeiza (EZE)");
        }
    }

    private static final List<String> AEROPUERTOS_BUENOS_AIRES = List.of("AEP", "EZE");

    /** Como en las aerolineas: dos vuelos que salen el mismo dia no pueden tener el mismo numero. */
    private void validarNumeroUnico(Vuelo vuelo) {
        LocalDateTime inicioDelDia = vuelo.getFechaSalida().toLocalDate().atStartOfDay();
        Long id = vuelo.getId() != null ? vuelo.getId() : 0L;
        if (vueloRepository.existeOtroEseDia(vuelo.getNumeroVuelo(), inicioDelDia, inicioDelDia.plusDays(1), id)) {
            throw new BadRequestException("Ya hay un vuelo " + vuelo.getNumeroVuelo() + " que sale ese dia");
        }
    }

    private Vuelo buscarPorId(Long id) {
        return vueloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vuelo no encontrado: " + id));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada: " + id));
    }

    private Aeropuerto buscarAeropuerto(String iata) {
        return aeropuertoRepository.findById(iata.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Aeropuerto no encontrado: " + iata));
    }
}
