package com.estebanmm13.auth_service.controllers;

import com.estebanmm13.auth_service.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints internos para comunicación entre microservicios")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}/username")
    @Operation(summary = "Obtener username por ID (uso interno entre microservicios)")
    public ResponseEntity<String> getUsernameById(@PathVariable Long id) {
        log.debug("Peticion interna - obteniendo username del usuario {}", id);
        String username = userService.findUserById(id).getUsername();
        return ResponseEntity.ok(username);
    }

}
