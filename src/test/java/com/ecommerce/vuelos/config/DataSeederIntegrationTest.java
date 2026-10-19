package com.ecommerce.vuelos.config;

import com.ecommerce.vuelos.IntegrationTestSupport;
import com.ecommerce.vuelos.entity.Vuelo;
import com.ecommerce.vuelos.repository.VueloRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DataSeederIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DataSeeder dataSeeder;
    @Autowired
    private VueloRepository vueloRepository;

    @Test
    void unVueloDeLaDemoQueYaSalio_vuelveAProgramarseEnElFuturo() throws Exception {
        Vuelo vuelo = vueloRepository.findFirstByNumeroVuelo("BC1402").orElseThrow();
        vuelo.setFechaSalida(LocalDateTime.now().minusDays(3));
        vuelo.setFechaLlegada(LocalDateTime.now().minusDays(3).plusHours(2));
        vueloRepository.saveAndFlush(vuelo);

        dataSeeder.run();

        Vuelo renovado = vueloRepository.findFirstByNumeroVuelo("BC1402").orElseThrow();
        assertTrue(renovado.getFechaSalida().isAfter(LocalDateTime.now()), "la salida tiene que quedar en el futuro");
        assertTrue(renovado.getFechaLlegada().isAfter(renovado.getFechaSalida()), "la llegada sigue despues de la salida");
    }
}
