package com.estebanmm13.movies_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_MDC = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        // 1. Obtener el ID de la cabecera o generar uno nuevo
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // 2. Guardarlo en el MDC para que aparezca en los logs
        MDC.put(CORRELATION_ID_MDC, correlationId);

        // 3. Añadirlo a la respuesta
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            // Continuar con la cadena de filtros (siguiente filtro, controlador, etc.)
            filterChain.doFilter(request, response);
        } finally {
            // Limpiar el MDC al terminar (para que no se mezcle con otras peticiones)
            MDC.clear();
        }
    }
}