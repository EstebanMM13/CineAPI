# AGENTS.md - Movies API (Microservicios)

## Instrucciones para asistentes de IA (Cursor, Copilot, Claude, etc.)

Eres un asistente de IA que ayuda a desarrollar y mantener esta API de microservicios. Sigue estas reglas estrictamente.

---

## 📐 Arquitectura

Este proyecto está compuesto por **5 microservicios** independientes basados en Spring Cloud:

```
PROYECTO_MOVIES/
├── service-registry/   → Eureka Server (puerto 8761)
├── config-server/      → Spring Cloud Config Server (puerto 8088)
├── api-gateway/        → Spring Cloud Gateway MVC (puerto 8060)
├── auth-service/       → Autenticación y usuarios (puerto 8081)
└── movies-service/     → Películas, géneros, reviews y votos (puerto 8082)
```

### Orden de arranque obligatorio
```
1. service-registry
2. config-server
3. auth-service + movies-service (pueden arrancar en paralelo)
4. api-gateway
```

### Comunicación entre servicios
- El cliente siempre envía el JWT en `Authorization: Bearer <token>`
- `movies-service` valida el JWT **localmente** con su propia copia de `JwtService`
- `auth-service` y `movies-service` **no se llaman entre sí** actualmente
- La comunicación futura entre servicios se hará via **OpenFeign** + **Circuit Breaker (Resilience4j)**

---

## 📚 Stack del proyecto

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Spring Cloud | 2025.1.1 |
| Seguridad | Spring Security + JWT (jjwt 0.11.5) |
| Persistencia | Spring Data JPA + Hibernate 6 |
| Base de datos | MySQL 8 (una por servicio) |
| Service Registry | Spring Cloud Netflix Eureka |
| Config Server | Spring Cloud Config (perfil native) |
| API Gateway | Spring Cloud Gateway Server WebMVC |
| Load Balancer | Spring Cloud LoadBalancer |
| Feign Client | Spring Cloud OpenFeign (pendiente de implementar) |
| Circuit Breaker | Resilience4j (pendiente de implementar) |
| Logging | SLF4J + Logback con correlationId |
| Documentación | Swagger/OpenAPI (springdoc-openapi) |
| Build | Maven |
| Contenedores | Docker + Docker Compose |

---

## 🗃️ Bases de datos

Cada microservicio tiene su **propia base de datos**:

| Servicio | BD | Puerto local | Puerto Docker |
|---|---|---|---|
| auth-service | `auth_db` | 3306 | 3307 |
| movies-service | `movies_db` | 3306 | 3308 |

**Reglas:**
- `ddl-auto: update` en desarrollo (Hibernate crea/actualiza tablas)
- Los scripts de inicialización están en `docker/init/`
- `auth_db` contiene: `users`
- `movies_db` contiene: `movies`, `genres`, `movie_genres`, `reviews`, `votes`
- **NUNCA** acceder a la BD de otro servicio directamente

---

## 🔐 JWT y seguridad

- El `auth-service` genera el JWT con `{sub: username, role: "USER"|"ADMIN", iat, exp}`
- El `movies-service` valida el JWT localmente con la misma `JWT_SECRET`
- Ambos servicios usan `EnumType.ORDINAL` para el rol: `USER=0`, `ADMIN=1`
- Prefijo de rol: `ROLE_USER`, `ROLE_ADMIN` (Spring Security standard)
- Endpoints públicos en `auth-service`: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`
- Endpoints públicos en `movies-service`: `/swagger-ui/**`, `/v3/api-docs/**`
- El resto requieren `Authorization: Bearer <token>`

---

## 🧱 Estructura de cada microservicio

```
src/main/java/com/estebanmm13/<servicio>/
├── config/         → Security, JWT, filtros, OpenAPI
├── controllers/    → Controladores REST (solo orquestación)
├── services/       → Interfaces de servicios
├── services/impl/  → Implementaciones con lógica de negocio
├── repositories/   → Repositorios JPA
├── models/         → Entidades JPA
├── dtoModels/      → DTOs separados en request/ y response/
├── mapper/         → Conversión entidad ↔ DTO
└── error/          → Excepciones personalizadas
```

---

## 🌐 Rutas del API Gateway

| Ruta | Servicio destino |
|---|---|
| `/api/auth/**` | auth-service |
| `/api/movies/**` | movies-service |
| `/api/genres/**` | movies-service |
| `/api/reviews/**` | movies-service |

---

## 🎯 Convenciones de código

- **Nombres de clases**: PascalCase (`MovieController`, `UserServiceImpl`)
- **Nombres de métodos y variables**: camelCase (`findMovieById`, `userRepository`)
- **Nombres de paquetes**: minúsculas con guión bajo (`movies_service`, `auth_service`)
- **DTOs**: Siempre separar Request (`*RequestDTO`) de Response (`*ResponseDTO`)
- **Entidades JPA**: Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- **Logging**: `@Slf4j` con niveles adecuados (INFO, DEBUG, WARN, ERROR)
- **No usar `User` de `org.apache.catalina`** — en `movies-service` usar `Long userId`

---

## 🚫 Reglas para agentes IA

1. **No exponer la entidad `User` directamente** → Siempre usar `UserResponseDTO` (sin password)
2. **No usar `System.out.println`** → Siempre `log.info(...)` o el nivel correspondiente
3. **No hardcodear credenciales** → Variables de entorno o `application.yaml` con `${...}`
4. **No usar `MySQL8Dialect`** → Hibernate 6 lo detecta automáticamente, no especificarlo
5. **No modificar el esquema en producción** → Usar `ddl-auto: validate`
6. **No cambiar la estructura sin consultar**
7. **Siempre incluir `correlationId` en los logs**
8. **En `movies-service` no añadir `UserRepository` ni `UserService`** → Los usuarios pertenecen a `auth-service`
9. **Los filtros con `@Component` deben tener `FilterRegistrationBean` con `setEnabled(false)`** para evitar doble registro en el security chain

---

## 📡 Logging y correlationId

- Cada petición tiene un `correlationId` (cabecera `X-Correlation-Id`)
- Se imprime en cada línea de log: `[%X{correlationId}]`
- Niveles por entorno:
    - `dev/local`: DEBUG para paquetes propios, WARN para librerías
    - `prod`: WARN/ERROR general

---

## 🐳 Docker

Cada servicio tiene su propio `Dockerfile` en la raíz del módulo. El `docker-compose.yml` está en la raíz del proyecto.

### Arrancar en Docker
```bash
# Primera vez (crea volúmenes y ejecuta init.sql)
docker compose up -d

# Con cambios en código (después de CI/CD)
docker compose pull
docker compose up -d

# Resetear BDs (borra datos)
docker compose down -v
docker compose up -d
```

### Variables de entorno en Docker
| Variable | Descripción |
|---|---|
| `EUREKA_URL` | URL del service-registry |
| `CONFIG_SERVER_URL` | URL del config-server |
| `DB_AUTH_URL` | JDBC URL de auth_db |
| `DB_MOVIES_URL` | JDBC URL de movies_db |
| `DB_USER` | Usuario MySQL |
| `DB_PASS` | Contraseña MySQL |
| `JWT_SECRET` | Clave secreta JWT (misma en todos los servicios) |

---

## 🔧 Configuración del config-server

Los archivos de configuración están en `config-server/src/main/resources/config/`:

```
config/
├── auth-service.yml      → Config de auth-service (BD, JWT, logging)
├── movies-service.yml    → Config de movies-service (BD, JWT, logging)
└── api-gateway.yml       → Config del gateway (puerto, rutas, Eureka)
```

El config-server usa `profile: native` y `search-locations: classpath:/config`.

---

## 🧪 Testing

- **Framework**: JUnit 5 + Mockito + Spring Boot Test
- **Ejecutar tests**: `mvn test` en cada módulo
- **Cobertura deseada**: Al menos un test por cada método público de servicio

---

## 📦 Comandos útiles

```bash
# Compilar un servicio
cd auth-service && mvn clean compile

# Ejecutar un servicio en local
cd auth-service && mvn spring-boot:run

# Compilar todos (desde raíz si hay pom padre)
mvn clean compile

# Ejecutar tests
mvn test
```

---

## ✨ Mejoras pendientes

- [ ] Feign Client en `movies-service` para obtener username de `auth-service`
- [ ] Circuit Breaker con Resilience4j
- [ ] Distributed Tracing (Zipkin)
- [ ] Frontend básico (React)
- [ ] Integración con TMDB API
- [ ] Sistema de recomendaciones
- [ ] Tests unitarios e integración