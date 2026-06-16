package com.estebanmm13.movies_service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", configuration = FeignClientConfig.class)
public interface AuthServiceClient {

    @GetMapping("/api/users/{id}/username")
    String getUsernameById(@PathVariable("id") Long id);
}
