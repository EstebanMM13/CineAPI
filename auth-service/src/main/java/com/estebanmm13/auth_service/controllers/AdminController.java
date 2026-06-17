package com.estebanmm13.auth_service.controllers;


import com.estebanmm13.auth_service.dtoModels.response.SystemStats;
import com.estebanmm13.auth_service.dtoModels.response.UserResponseDTO;
import com.estebanmm13.auth_service.models.Role;
import com.estebanmm13.auth_service.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints exclusivos para administradores")
@SecurityRequirement(name = "bearer-auth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Returns a paginated list of all users. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by", example = "id") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction: asc or desc", example = "asc") @RequestParam(defaultValue = "asc") String direction) {

        log.info("Admin - Obteniendo usuarios - página: {}, tamaño: {}", page, size);

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        return ResponseEntity.ok(userService.findAllUsers(pageable));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponseDTO> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        log.info("Admin - Obteniendo usuario con ID: {}", id);
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user by ID", description = "Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        log.info("Admin - Eliminando usuario con ID: {}", id);
        userService.deleteUser(id);
        log.info("Usuario eliminado exitosamente: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get system stats", description = "Returns total, admin and regular user counts. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Stats retrieved successfully")
    public ResponseEntity<SystemStats> getSystemStats() {
        log.info("Admin - Obteniendo estadísticas del sistema");

        long totalUsers = userService.countUsers();
        long adminUsers = userService.countUsersByRole(Role.ADMIN);
        long regularUsers = userService.countUsersByRole(Role.USER);

        SystemStats stats = SystemStats.builder()
                .totalUsers(totalUsers)
                .adminUsers(adminUsers)
                .regularUsers(regularUsers)
                .build();

        return ResponseEntity.ok(stats);
    }
}