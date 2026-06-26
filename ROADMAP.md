# Roadmap — CineAPI (microservicios)

Objetivo: convertir este proyecto en un ejemplo sólido de arquitectura de microservicios para el CV, capaz de defenderse en entrevista técnica.

**Estado actual:** Fase 1 y parte de Fase 3 completadas. El siguiente paso prioritario es **Kafka (punto 6)** — es lo que más diferencia el CV del resto de juniors y lo que cierra el hueco de "solo comunicación síncrona".

---

## ✅ Fase 1 — Núcleo "esto es de verdad microservicios"

### 1. Comunicación real entre servicios con Feign — HECHO Y VERIFICADO

`movies-service` llama a `auth-service` para resolver el username del autor en cada review. Verificado en docker-compose: reviews devuelven `username` real.

Ficheros clave:
- `movies-service/.../clients/AuthServiceClient.java` — interfaz @FeignClient, endpoint `GET /api/users/{id}/username`
- `movies-service/.../clients/FeignClientConfig.java` — RequestInterceptor que propaga el header `Authorization`
- `movies-service/.../clients/UsernameResolver.java` — @Component con @CircuitBreaker + @Retry + fallback
- `movies-service/.../dtoModels/response/ReviewResponseDTO.java` — campo `username`
- `movies-service/.../services/review/ReviewServiceImpl.java` — inyecta `UsernameResolver`

### 2. Circuit Breaker + Retry con Resilience4j — HECHO Y VERIFICADO

Verificado en docker-compose: al tirar auth-service, las primeras peticiones intentan (Retry), las siguientes son instantáneas (circuito abierto), devuelven 200 con `"Usuario desconocido"`.

Config en `config-server/src/main/resources/config/movies-service.yml`:
- Circuit breaker `auth-service-cb`: sliding-window 5, threshold 60%, open 10s
- Retry `auth-service-retry`: max 2 intentos, 200ms espera

Lección importante: @CircuitBreaker/@Retry usan AOP. La self-invocation bypasea el proxy — solución: extraer a @Component separado (`UsernameResolver`).

### 3. Sistema de votación (ratings) — HECHO

Modelo `Vote` con restricción única `(user_id, movie_id)` — un voto por usuario por película. El campo `rating` y `votes` se almacenan directamente en `Movie`. Controladores de reviews y votes implementados con validación y manejo de errores.

### 4. RBAC (USER / ADMIN) — HECHO

- `AdminController` con `@PreAuthorize("hasRole('ADMIN')")`: listar usuarios (paginado), obtener por ID, eliminar, estadísticas del sistema.
- `SecurityConfig` independiente en auth-service y movies-service (ambos validan JWT con secreto compartido HS256).

### 5. Logging con correlationId — HECHO

`CorrelationIdFilter` en auth-service y movies-service. Patrón de log incluye `[%X{correlationId}]`. Base lista para Zipkin (punto 9).

---

## Fase 2 — Comunicación asíncrona (SIGUIENTE, PRIORITARIO)

### 6. Kafka — PENDIENTE

**Caso de uso propuesto:** "review creada → evento `ReviewCreatedEvent` → movies-service recalcula el rating medio de la película" (desacopla la escritura del recálculo).

**Por qué es prioritario:** cierra el hueco de "solo comunicación síncrona". En entrevista permite hablar de cuándo usar Feign (necesito respuesta inmediata) vs Kafka (no necesito respuesta, desacoplo, escalo). Muy pocos juniors tienen esto.

**Alternativa más sencilla si el tiempo apremia:** RabbitMQ. Menos habitual en empresas grandes, pero mucho más rápido de implementar.

**Pasos concretos para Kafka:**
1. Añadir Kafka a `docker-compose.yml` (imagen `confluentinc/cp-kafka` + Zookeeper o KRaft).
2. Dependencia `spring-kafka` en movies-service.
3. Crear `ReviewCreatedEvent` (record/DTO serializable a JSON).
4. Producer en `ReviewServiceImpl` tras persistir la review.
5. Consumer en movies-service (mismo servicio, otro listener) que recalcula y guarda el rating.
6. Config del topic en `movies-service.yml`.

---

## Fase 3 — Observabilidad

### 7. Actuator + health checks — HECHO

- `/actuator/health` expuesto en auth-service y movies-service.
- Usado en los healthchecks del docker-compose para orquestar el arranque correcto de los contenedores.

### 8. Métricas con Prometheus + Grafana — HECHO

- Actuator expone `/actuator/prometheus` en ambos servicios.
- Prometheus rasca cada 15s (config en `monitoring/prometheus.yml`).
- Grafana en puerto 3000 con datasource provisionado automáticamente.
- Redis Cache integrado en movies-service: géneros (TTL 1h), género por ID (TTL 1h), película por ID (TTL 15min).
- Métricas de caché (`cache_gets_total`) disponibles en Prometheus.
- Dashboard "Spring Boot 3.x Statistics" (ID 19004) importado y funcionando.

### 9. Distributed tracing con Micrometer Tracing + Zipkin — PENDIENTE

- `CorrelationIdFilter` ya existe en ambos servicios — es el salto natural.
- Añadir `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` + Zipkin en docker-compose.
- Esfuerzo medio.

---

## Fase 4 — Seguridad

### 10. JWT con clave asimétrica RS256 — PENDIENTE (opcional)

- Ahora mismo: secreto HS256 compartido entre servicios (en config-server YMLs). Funciona pero no es lo ideal.
- Mejora: auth-service firma con clave privada, movies-service valida con clave pública (no necesita el secreto).
- Esfuerzo medio. Solo merece la pena si se quiere hablar de seguridad en la entrevista.

### 11. Rate limiting en el Gateway — PENDIENTE

- Spring Cloud Gateway MVC soporta filtros de rate limiting con Redis.
- Esfuerzo medio. Buena tarjeta de CV si se combina con el Kubernetes del punto 12.

---

## Fase 5 — Despliegue

### 12. Kubernetes manifests básicos — PENDIENTE

- Deployments + Services para cada módulo, ConfigMap para variables de entorno, Secret para JWT_SECRET y credenciales de BD.
- Esfuerzo alto. Máximo impacto CV si se combina con un CI/CD (GitHub Actions).

### 13. CI/CD con GitHub Actions — HECHO

- Pipeline en `.github/workflows/ci-cd.yml`: build → test → docker build → push a Docker Hub.
- Imágenes publicadas en Docker Hub bajo `estebanmm13/*`.

---

## Cosas pendientes fuera del código

- **Actualizar CV** con lenguaje de microservicios: Eureka, Feign, Circuit Breaker, Resilience4j, Kafka (cuando esté). Ya hay base suficiente para hacerlo ahora.
- **Actualizar README** explicando la arquitectura (diagrama de servicios, tabla de rutas, cómo correr en local y con Docker). Lo lee el técnico antes de la entrevista.
- **Añadir ruta gateway para `/api/v1/reviews/**`** — `ReviewUsersController` usa `/api/reviews/{userId}` pero hay que verificar que el predicado del gateway cubre este path (actualmente solo está `/api/v1/reviews/**` para `movies-service`).
