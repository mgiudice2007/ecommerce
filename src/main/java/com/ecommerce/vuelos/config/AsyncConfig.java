package com.ecommerce.vuelos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita @Async: el mail de confirmacion se manda en otro hilo para no demorar la respuesta del checkout. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
