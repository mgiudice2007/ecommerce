package com.ecommerce.vuelos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;


@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginaResponse<T> {

    private final List<T> content;
    private final int number;
    private final int size;
    private final int numberOfElements;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;
    private final boolean empty;
    private final String mensaje;

    private PaginaResponse(Page<T> page, String mensaje) {
        this.content = page.getContent();
        this.number = page.getNumber();
        this.size = page.getSize();
        this.numberOfElements = page.getNumberOfElements();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
        this.empty = page.isEmpty();
        this.mensaje = mensaje;
    }

    public static <T> PaginaResponse<T> desde(Page<T> page, String mensajeSiVacia) {
        return new PaginaResponse<>(page, page.isEmpty() ? mensajeSiVacia : null);
    }
}
