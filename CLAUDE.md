# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Movies API: a Spring Cloud microservices backend for managing movies, genres, reviews and ratings, with JWT auth and role-based access (USER/ADMIN). Multi-module Maven project, Java 17, Spring Boot 3.5.3, Spring Cloud 2023.0.0 (see root [pom.xml](pom.xml) — ignore the version badges in README.md, they're stale/aspirational).

## Modules

- `service-registry` (8761) — Eureka service discovery
- `config-server` (8088) — Spring Cloud Config, native profile, configs live in `config-server/src/main/resources/config/*.yml` (one file per service: `auth-service.yml`, `movies-service.yml`, `api-gateway.yml`)
- `api-gateway` (8060) — Spring Cloud Gateway MVC, single entry point, routes by path predicate to `lb://auth-service` / `lb://movies-service` via Eureka
- `auth-service` (8081) — registration, login (JWT issuance), user/admin management, own MySQL db (`auth_db`)
- `movies-service` (8082) — movies, genres, reviews, votes, own MySQL db (`movies_db`)

Each business service has its own database — no shared schema/joins across `auth-service` and `movies-service`. Cross-service lookups (e.g. movies-service needing a username for a review) go through a Feign client (`AuthServiceClient` in movies-service) calling auth-service's exposed endpoints, registered via Eureka.

## Build / run / test

Build everything from root:
```
mvn clean install
```

Run a single module's tests:
```
cd movies-service && mvn test
cd movies-service && mvn test -Dtest=MovieControllerTest
```

Run the full stack locally (requires local MySQL with `auth_db` and `movies_db` created), start in this order — config-server and service-registry must be up before the business services and gateway:
```
cd service-registry  && mvn spring-boot:run
cd config-server     && mvn spring-boot:run
cd auth-service      && mvn spring-boot:run
cd movies-service    && mvn spring-boot:run
cd api-gateway       && mvn spring-boot:run
```

Or via Docker (images start in the correct dependency order automatically):
```
docker compose up -d
```

Key env vars (see [docker-compose.yml](docker-compose.yml)): `JWT_SECRET`, `DB_AUTH_URL`, `DB_MOVIES_URL`, `DB_USER`, `DB_PASS`, `EUREKA_URL`, `CONFIG_SERVER_URL`.

## Routing — IMPORTANT, easy to get wrong

Public-facing paths go through the gateway with an `/api/v1/...` prefix; the gateway rewrites them to the internal `/api/...` paths the controllers actually use (see [config-server/src/main/resources/config/api-gateway.yml](config-server/src/main/resources/config/api-gateway.yml)):

```yaml
- id: auth-service
  predicates: [Path=/api/v1/auth/**, /api/v1/users/**, /api/v1/admin/**]
  filters: [RewritePath=/api/v1/(?<segment>.*), /api/${segment}]
- id: movies-service
  predicates: [Path=/api/v1/movies/**, /api/v1/genres/**, /api/v1/reviews/**]
  filters: [RewritePath=/api/v1/(?<segment>.*), /api/${segment}]
```

Internal controller paths (no version, unchanged):
- `AuthController` → `/api/auth` (public, register/authenticate)
- `UserController` → `/api/users`
- `AdminController` → `/api/admin` (ROLE_ADMIN only)
- `MovieController` → `/api/movies`
- `GenreController` → `/api/genres`
- `ReviewController` → `/api/movies/{movieId}/reviews`
- `ReviewUsersController` → `/api/reviews/{userId}`

**Whenever you add a controller or change a `@RequestMapping` prefix, you must also update the gateway's `Path=` predicate in `api-gateway.yml`.** The gateway does not infer routes from the services — it only forwards prefixes it's explicitly told about. This has bitten the project before: `/api/users` and `/api/admin` existed in `auth-service` for a while with no matching gateway route, making them unreachable from outside.

## Security

Both `auth-service` and `movies-service` have their own `SecurityConfig` (stateless JWT, `JwtFilter` added before `UsernamePasswordAuthenticationFilter`).
- `movies-service`: `GET /api/movies/**`, `/api/genres/**`, `/api/reviews/**` are `permitAll()` (public read access, no JWT needed); everything else — writes, and swagger/actuator health endpoints aside — requires a valid JWT via `anyRequest().authenticated()`.
- `auth-service`: `/api/auth/**` and swagger are `permitAll()`; everything else (including `/api/users`, `/api/admin`) requires a JWT. `AdminController` additionally requires `ROLE_ADMIN` via `@PreAuthorize`.

JWT secret is shared across services (`jwt.secret` in each service's config-server YAML) so tokens issued by auth-service validate in movies-service.

## Testing conventions

`movies-service` has the fuller test suite: `@WebMvcTest` + Mockito for controllers (e.g. [MovieControllerTest.java](movies-service/src/test/java/com/estebanmm13/movies_service/controllers/MovieControllerTest.java)), plus repository tests and service-impl tests. Follow this same `@WebMvcTest`-with-mocked-service pattern for new controller tests rather than full `@SpringBootTest`.

## API docs

Swagger UI per service (not exposed through the gateway): `http://localhost:8081/swagger-ui.html` (auth-service), `http://localhost:8082/swagger-ui.html` (movies-service).