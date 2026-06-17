<div align="center">

# 🎬 CineAPI

### REST API built with Spring Cloud Microservices for movie management

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.3-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2023.0.0-brightgreen?style=flat-square&logo=spring)](https://spring.io/projects/spring-cloud)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-ready-blue?style=flat-square&logo=docker)](https://www.docker.com/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-black?style=flat-square&logo=githubactions)](https://github.com/features/actions)

</div>

---

## 📖 About

CineAPI is a backend application built on a **microservices architecture** that allows managing movies, genres, reviews and ratings. It includes a full authentication system with **JWT** and role-based access control (USER / ADMIN).

Inter-service communication is handled via **Feign Client** with **Circuit Breaker + Retry (Resilience4j)** for fault tolerance and automatic fallback. JWT tokens are automatically propagated between services through a Feign `RequestInterceptor`, and each service validates them independently.

The project follows **Domain-Driven Design** principles, separation of concerns and Spring Cloud best practices.

---

## 🏗️ Architecture

```
                        ┌─────────────────┐
                        │   API Gateway   │
                        │     :8060       │
                        └────────┬────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
           ┌────────▼────────┐     ┌──────────▼────────┐
           │  Auth Service   │     │  Movies Service   │
           │    :8081        │     │    :8082          │
           └────────┬────────┘     └──────────┬────────┘
                    │                         │
           ┌────────▼────────┐     ┌──────────▼────────┐
           │    auth_db      │     │    movies_db      │
           │  MySQL :3307    │     │  MySQL :3308      │
           └─────────────────┘     └───────────────────┘

           ┌─────────────────┐     ┌───────────────────┐
           │Service Registry │     │  Config Server    │
           │  Eureka :8761   │     │    :8088          │
           └─────────────────┘     └───────────────────┘
```

### Services

| Service | Port | Description |
|---|---|---|
| `api-gateway` | 8060 | Single entry point, dynamic routing via Eureka |
| `auth-service` | 8081 | User registration, login and management |
| `movies-service` | 8082 | Movies, genres, reviews and ratings |
| `config-server` | 8088 | Centralized configuration (Spring Cloud Config) |
| `service-registry` | 8761 | Service discovery (Eureka) |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.3 |
| Microservices | Spring Cloud 2023.0.0 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | MySQL 8.0 (one per service) |
| Service Registry | Netflix Eureka |
| Config Server | Spring Cloud Config (native profile) |
| API Gateway | Spring Cloud Gateway MVC |
| Inter-service Comm. | Feign Client + RequestInterceptor (JWT propagation) |
| Fault Tolerance | Resilience4j (Circuit Breaker + Retry) |
| Load Balancer | Spring Cloud LoadBalancer |
| Containers | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| Documentation | Swagger / OpenAPI 3 |
| Build Tool | Maven |

---

## 🚀 Quick Start

### Requirements

- Docker and Docker Compose
- Git

### Run with Docker

```bash
# Clone the repository
git clone https://github.com/EstebanMM13/CineAPI.git
cd CineAPI

# Start all services
docker compose up -d

# Check containers status
docker ps
```

Services start automatically in the correct order:
1. MySQL (auth + movies)
2. Service Registry (Eureka)
3. Config Server
4. Auth Service + Movies Service
5. API Gateway

### Run locally (development)

```bash
# Start in order
cd service-registry  && mvn spring-boot:run
cd config-server     && mvn spring-boot:run
cd auth-service      && mvn spring-boot:run
cd movies-service    && mvn spring-boot:run
cd api-gateway       && mvn spring-boot:run
```

Requires MySQL running locally with `auth_db` and `movies_db` databases created.

---

## 🔐 Authentication

The API uses **JWT Bearer Token**. Flow:

```bash
# 1. Register a user
POST /api/auth/register
{
  "username": "john",
  "email": "john@example.com",
  "password": "password123"
}

# 2. Get token
POST /api/auth/authenticate
{
  "username": "john",
  "password": "password123"
}
# Response: { "token": "eyJhbGci..." }

# 3. Use the token in requests
GET /api/movies
Authorization: Bearer eyJhbGci...
```

### Roles

| Role | Access |
|---|---|
| `USER` | Read movies, write reviews, vote |
| `ADMIN` | All above + create, update and delete movies and genres |

---

## 📡 API Endpoints

### Auth Service

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Register new user | ❌ |
| POST | `/api/auth/authenticate` | Login, returns JWT | ❌ |

### Movies

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/movies` | Get paginated movies list | ✅ |
| GET | `/api/movies/{id}` | Get movie by ID | ✅ |
| GET | `/api/movies/title?title=` | Search by title | ✅ |
| GET | `/api/movies/genre/{name}` | Filter by genre | ✅ |
| POST | `/api/movies` | Create movie | 🔐 ADMIN |
| PUT | `/api/movies/{id}` | Update movie | 🔐 ADMIN |
| DELETE | `/api/movies/{id}` | Delete movie | 🔐 ADMIN |
| PUT | `/api/movies/{id}/vote/{userId}/{rating}` | Rate a movie | ✅ |

### Reviews

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/movies/{movieId}/reviews` | Get movie reviews | ✅ |
| POST | `/api/movies/{movieId}/reviews` | Create review | ✅ |
| PUT | `/api/movies/{movieId}/reviews/{id}` | Update review | ✅ |
| DELETE | `/api/movies/{movieId}/reviews/{id}` | Delete review | ✅ |

### Genres

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/genres` | List all genres | ✅ |
| POST | `/api/genres` | Create genre | 🔐 ADMIN |

---

## 📚 API Documentation

Swagger UI available on each service:

| Service | URL |
|---|---|
| Auth Service | `http://localhost:8081/swagger-ui.html` |
| Movies Service | `http://localhost:8082/swagger-ui.html` |

## 🌐 Interactive Documentation (DeepWiki)

Explore the automatically generated documentation for this project at [DeepWiki](https://deepwiki.com/EstebanMM13/CineAPI).

---

## 🐳 Docker

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `JWT_SECRET` | Secret key for signing JWT | dev default value |
| `DB_AUTH_URL` | JDBC URL for auth_db | `localhost:3306/auth_db` |
| `DB_MOVIES_URL` | JDBC URL for movies_db | `localhost:3306/movies_db` |
| `DB_USER` | MySQL username | `root` |
| `DB_PASS` | MySQL password | `1234` |
| `EUREKA_URL` | Service registry URL | `localhost:8761/eureka/` |
| `CONFIG_SERVER_URL` | Config server URL | `localhost:8088` |

### Useful Commands

```bash
# Start everything
docker compose up -d

# Follow logs of a service
docker logs movies-auth-service -f

# Restart without losing data
docker compose down
docker compose pull
docker compose up -d

# Full reset (deletes all data)
docker compose down -v
docker compose up -d
```

---

## 🔄 CI/CD Pipeline

GitHub Actions pipeline runs on every push to `master`:

1. **Build** — compiles all 5 services with Maven (with dependency caching)
2. **Docker** — builds and pushes images to Docker Hub

Images available at [Docker Hub](https://hub.docker.com/u/estebanmm13).

---

## 📁 Project Structure

```
CineAPI/
├── api-gateway/                  # Spring Cloud Gateway MVC
├── auth-service/                 # Authentication & user management
│   └── src/main/java/
│       ├── config/               # Security, JWT, filters
│       ├── controllers/          # AuthController, AdminController
│       ├── services/             # AuthService, UserService
│       ├── models/               # User, Role
│       └── repositories/        # UserRepository
├── movies-service/               # Movies, genres, reviews
│   └── src/main/java/
│       ├── config/               # Security, JWT, filters
│       ├── controllers/          # MovieController, GenreController, ReviewController
│       ├── services/             # MovieService, GenreService, ReviewService
│       ├── models/               # Movie, Genre, Review, Vote
│       └── repositories/        # JPA Repositories
├── config-server/                # Centralized configuration
│   └── src/main/resources/
│       └── config/               # Per-service YML files
├── service-registry/             # Eureka Server
├── docker-compose.yml            # Container orchestration
└── .github/workflows/            # GitHub Actions CI/CD
```

---

## 🗺️ Roadmap

- [x] Feign Client for inter-service communication
- [x] Circuit Breaker with Resilience4j
- [x] Unit and integration tests
- [ ] Distributed Tracing with Zipkin
- [ ] React frontend
- [ ] TMDB API integration

---

## 👨‍💻 Author

**Esteban** — [@estebanmm13](https://github.com/estebanmm13)
