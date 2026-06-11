package com.estebanmm13.api_gateway.config;

import org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> authServiceRoute() {
        return GatewayRouterFunctions.route("auth-service")
                .route(RequestPredicates.path("/api/auth/**"),
                        HandlerFunctions.http())
                .filter(LoadBalancerFilterFunctions.lb("auth-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> moviesServiceRoute() {
        return GatewayRouterFunctions.route("movies-service")
                .route(RequestPredicates.path("/api/movies/**")
                                .or(RequestPredicates.path("/api/genres/**"))
                                .or(RequestPredicates.path("/api/reviews/**")),
                        HandlerFunctions.http())
                .filter(LoadBalancerFilterFunctions.lb("movies-service"))
                .build();
    }
}