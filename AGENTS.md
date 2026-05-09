# Arkive Java Advanced Instructions

## 1. Project Context

- Arkive is a FIAP Challenge 2026 Java Advanced project for CLYVO VET.
- It is a Spring Boot REST API for a continuous pet health journey MVP.
- This is a school project, not an enterprise production system.

## 2. Academic Complexity Rules

Use simple, explainable code with:

- Spring Boot REST controllers
- Services
- Spring Data JPA repositories
- JPA entities
- DTOs
- Bean Validation
- Simple exception handling
- Pagination, sorting and search
- Postman-friendly endpoints

Do not use these unless explicitly requested:

- Hexagonal architecture
- CQRS
- Event sourcing
- Kafka/message brokers
- Spring Security/JWT for now
- Complex authentication/authorization
- MapStruct
- Sockets
- Async processing
- Microservices
- Complex generic abstractions
- Unnecessary inheritance
- Unnecessary bidirectional relationships

## 3. Database Rules

- The Oracle schema is frozen and is the source of truth.
- Do not create, rename, or change tables, columns, relationships, or constraints.
- Do not add Flyway/Liquibase unless explicitly requested.
- Do not configure Hibernate to create, update, or drop the schema.
- When Oracle is configured, prefer `spring.jpa.hibernate.ddl-auto=validate`.

## 4. Java/Spring Rules

- Java 17.
- Spring Boot 3.5.x.
- Use `jakarta.persistence.*` and `jakarta.validation.*`.
- Do not use `javax.persistence.*` or `javax.validation.*`.
- Keep package structure:
  - `br.com.fiap.arkive.config`
  - `br.com.fiap.arkive.controller`
  - `br.com.fiap.arkive.dto.request`
  - `br.com.fiap.arkive.dto.response`
  - `br.com.fiap.arkive.entity`
  - `br.com.fiap.arkive.exception`
  - `br.com.fiap.arkive.repository`
  - `br.com.fiap.arkive.service`

## 5. Profiles

- The `local-nodb` profile must continue to work before Oracle is ready.
- This command must keep working:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-nodb"
```

- `GET /api/health` must keep working under `local-nodb`.
- Database-backed controllers/services should not break `local-nodb`.

## 6. Git Workflow

- Do not commit directly to `main`.
- Use small feature branches.
- Before committing, run:

```powershell
git status
.\mvnw.cmd clean package
```

- Confirm `target/` is not staged.
- Commit only intended files.
- Push the feature branch.
- Do not merge into `main` unless explicitly requested.

## 7. MVP Scope

The Java MVP is implemented one branch at a time:

- Especie
- Raca
- Clinica
- Veterinario
- Responsavel
- Animal
- AnimalResponsavel
- Consulta
- Diagnostico
- Prescricao
- AdesaoPrescricao
- AvaliacaoBemEstar
- ProtocoloPreventivo
- EventoPreventivo
- Alerta
- FeedbackNps
- EventoJornada

Do not implement full authentication, full user roles, WhatsApp integration, real AI diagnosis, dashboard, data lakehouse, payments, or LTV calculation in this phase.

## 8. Done Criteria

A task is done only when:

- Code compiles.
- Tests pass.
- `local-nodb` still works when applicable.
- Endpoints are simple and testable.
- No unrelated files changed.
- No schema changes were introduced.
- The implementation can be explained in class.
