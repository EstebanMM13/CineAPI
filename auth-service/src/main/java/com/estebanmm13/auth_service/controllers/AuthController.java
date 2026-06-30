package com.estebanmm13.auth_service.controllers;


import com.estebanmm13.auth_service.dtoModels.request.AuthenticationRequest;
import com.estebanmm13.auth_service.dtoModels.request.RegisterRequest;
import com.estebanmm13.auth_service.dtoModels.response.AuthResponse;
import com.estebanmm13.auth_service.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns a JWT token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid registration data"),
            @ApiResponse(responseCode = "403", description = "Email already exist"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Parameter(description = "User registration data", required = true)
            @Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Solicitud de registro para usuario: {}", registerRequest.getUsername());
        AuthResponse response = authService.register(registerRequest);
        log.info("Usario registrado exitosamente: {}", registerRequest.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Authenticate user",
            description = "Authenticates user credentials and returns a JWT token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticate(
            @Parameter(description = "User credentials", required = true)
            @Valid @RequestBody AuthenticationRequest authenticationRequest) {
        log.debug("Intento de login para usuario: {}",authenticationRequest.getUsername());
        AuthResponse response = authService.authenticate(authenticationRequest);
        log.debug("Usuario auntenticado: {}",authenticationRequest.getUsername());
        return ResponseEntity.ok(response);
    }
}