---
name: fiap-java-advanced-feature
description: Use this skill when implementing one controlled feature branch for the Arkive FIAP Java Advanced Spring Boot API. It keeps work simple, academic, database-safe, and aligned with the frozen Oracle schema. Do not use for DevOps, Docker, Azure, documentation-only tasks, or frontend/mobile tasks.
---

# FIAP Java Advanced Feature

## 1. Purpose

- Implement one small Arkive Java Advanced feature branch at a time.
- Keep the implementation explainable for a FIAP Java Advanced class.
- Follow `AGENTS.md` first, then this skill.

## 2. Required Workflow

- Confirm current branch and task scope.
- Inspect existing files before editing.
- Do not touch unrelated files.
- Do not modify database schema.
- Implement only requested resources.
- Run build/tests.
- Report files changed and build result.
- Do not commit unless explicitly requested.

## 3. Allowed Implementation Style

- Spring Boot REST controllers.
- Services.
- Spring Data JPA repositories.
- JPA entities.
- DTO request/response classes.
- Bean Validation with `jakarta.validation`.
- Simple exception handling.
- Pageable pagination and simple search filters.
- Postman-friendly endpoints.

## 4. Forbidden Complexity Unless Explicitly Requested

- Spring Security/JWT.
- Hexagonal architecture.
- CQRS.
- Event sourcing.
- Kafka/message brokers.
- MapStruct.
- Sockets.
- Async processing.
- Microservices.
- Complex generic abstractions.
- Unnecessary inheritance.
- Unnecessary bidirectional relationships.

## 5. Database Safety

- Treat the Oracle schema as frozen.
- Map to existing tables/columns exactly.
- Do not create, rename, or change tables/columns.
- Do not add Flyway or Liquibase.
- Do not configure Hibernate to create/update/drop schema.
- Prefer `ddl-auto=validate` when Oracle config exists.
- Use `jakarta.persistence` imports only.

## 6. local-nodb Safety

- Preserve `local-nodb`.
- This must continue working:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local-nodb"
```

- `GET /api/health` must keep working.
- Database-backed beans must not break `local-nodb`.

## 7. Done Checklist

- Code compiles.
- Tests pass.
- `local-nodb` still works when applicable.
- No unrelated files changed.
- No schema changes.
- Feature is small and explainable.
- Summary includes endpoints, validations and files changed.
