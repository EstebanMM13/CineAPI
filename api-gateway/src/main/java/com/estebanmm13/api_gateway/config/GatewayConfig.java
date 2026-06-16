package com.estebanmm13.api_gateway.config;

import org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions;
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
                .route(RequestPredicates.path("/api/v1/auth/**")
                                .or(RequestPredicates.path("/api/v1/users/**"))
                                .or(RequestPredicates.path("/api/v1/admin/**")),
                        HandlerFunctions.http())
                .filter(FilterFunctions.rewritePath("/api/v1/(?<segment>.*)", "/api/${segment}"))
                .filter(LoadBalancerFilterFunctions.lb("auth-service"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> moviesServiceRoute() {
        return GatewayRouterFunctions.route("movies-service")
                .route(RequestPredicates.path("/api/v1/movies/**")
                                .or(RequestPredicates.path("/api/v1/genres/**"))
                                .or(RequestPredicates.path("/api/v1/reviews/**")),
                        HandlerFunctions.http())
                .filter(FilterFunctions.rewritePath("/api/v1/(?<segment>.*)", "/api/${segment}"))
                .filter(LoadBalancerFilterFunctions.lb("movies-service"))
                .build();
    }
}