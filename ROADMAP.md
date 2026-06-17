# Roadmap — Movies Platform (microservicios)

Objetivo: convertir este proyecto en un ejemplo sólido de arquitectura de microservicios para el CV, capaz de defenderse en entrevista técnica.

El siguiente paso prioritario es **Kafka (punto 6)** — es lo que más diferencia el CV del resto de juniors y lo que cierra el hueco de "solo comunicación síncrona".

---

## ✅ Fase 1 — Núcleo "esto es de verdad microservicios"

### 1. Comunicación real entre servicios con Feign — HECHO Y VERIFICADO

`movies-service` llama a `auth-service` para resolver el username del autor en cada review. Verificado en docker-compose: reviews devuelven `username` real.

Ficheros tocados:
- `movies-service/src/main/java/.../clients/AuthServiceClient.java` — interfaz @FeignClient apuntando a `auth-service`, endpoint `GET /api/users/{id}/username`
- `movies-service/src/main/java/.../clients/FeignClientConfig.java` — RequestInterceptor que copia el header `Authorization` de la petición entrante a la saliente (necesario porque auth-service exige JWT)
- `movies-service/src/main/java/.../clients/UsernameResolver.java` — @Component que encapsula la llamada Feign + @CircuitBreaker + @Retry + fallback (ver punto 2)
- `movies-service/src/main/java/.../dtoModels/response/ReviewResponseDTO.java` — añadido campo `username`
- `movies-service/src/main/java/.../mapper/ReviewMapper.java` — ajustado constructor (username=null, lo rellena el servicio)
- `movies-service/src/main/java/.../services/review/ReviewServiceImpl.java` — inyecta `UsernameResolver`, llama a `resolveUsername()` en `toResponseDTOWithUsername()`
- `movies-service/src/test/java/.../services/review/ReviewServiceImplTest.java` — añadido `@Mock UsernameResolver`

### 2. Circuit Breaker + Retry con Resilience4j — HECHO Y VERIFICADO

Verificado en docker-compose: al tirar auth-service, las primeras peticiones tardan (Retry intentando), las siguientes son instantáneas (circuito abierto), devuelven 200 OK con `"Usuario desconocido"`.

Ficheros tocados:
- `config-server/src/main/resources/config/movies-service.yml` — configuración de instancias `auth-service-cb` (sliding-window 5, threshold 60%, open 10s) y `auth-service-retry` (max 2 intentos, 200ms espera)
- `movies-service/src/main/java/.../clients/UsernameResolver.java` — @CircuitBreaker + @Retry sobre `resolveUsername()`, fallback `usernameFallback()` que devuelve `"Usuario desconocido"`
- `movies-service/src/main/java/.../error/GlobalExceptionHandler.java` — eliminado @ExceptionHandler(FeignException.class) que interfería con Resilience4j

Lección importante aprendida: las anotaciones @CircuitBreaker/@Retry usan Spring AOP (proxies). Si el método anotado está en la misma clase que quien lo llama (self-invocation), el proxy se bypasea y las anotaciones no funcionan. Solución: extraer el método a un @Component separado (UsernameResolver) para que la llamada pase por el proxy.

---

## Fase 2 — Comunicación asíncrona (SIGUIENTE, PRIORITARIO)

### 6. Kafka o RabbitMQ — PENDIENTE

Caso de uso: "review creada → evento → recalcular rating medio de la película en movies-service" (o alternativa: "usuario registrado en auth-service → evento → notificación/perfil en movies-service").

Por qué es prioritario: cierra el hueco de "solo comunicación síncrona". En entrevista permite hablar de cuándo usar síncrono (Feign, necesito respuesta inmediata) vs asíncrono (Kafka, no necesito respuesta inmediata, desacoplo los servicios). Muy pocos juniors tienen esto.

Decisión pendiente: Kafka (más complejo, más habitual en empresas grandes) vs RabbitMQ (más simple, más habitual en empresas medianas). Consultar al usuario qué prefiere.

---

## Fase 3 — Observabilidad

### 3. Actuator + health checks — PENDIENTE
- Exponer `/actuator/health` en cada servicio.
- Esfuerzo bajo.

### 4. Distributed tracing con Micrometer Tracing + Zipkin — PENDIENTE
- Ya existe `CorrelationIdFilter` en ambos servicios, es el salto natural.
- Esfuerzo medio.

### 5. JWT con clave asimétrica RS256 — PENDIENTE (opcional)
- Ahora mismo secreto HS256 compartido entre servicios.
- Mejora de seguridad, medio esfuerzo.

---

## Fase 4 — Pulido

### 7. Kubernetes manifests básicos — PENDIENTE
### 8. Rate limiting en el Gateway — PENDIENTE

---

## Cosas pendientes fuera del código

- Actualizar CV (Backend Java y Full Stack) con lenguaje de microservicios: Eureka, Feign, Circuit Breaker, CI/CD. Ya hay base suficiente para hacerlo.
- Actualizar README del proyecto explicando la arquitectura — lo lee el técnico antes de la entrevista.
