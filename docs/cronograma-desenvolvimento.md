# Cronograma de Desenvolvimento — Arkive API

Este documento apresenta, de forma resumida, a divisão de responsabilidades da equipe e o cronograma principal de desenvolvimento do projeto **Arkive**, desenvolvido para o **Challenge FIAP 2026 — CLYVO VET**.

---

## 1. Equipe e responsabilidades

| Integrante | Responsabilidades |
| --- | --- |
| Gustavo | Desenvolvimento da API Java com Spring Boot, integração com Oracle, Swagger, testes de endpoints, documentação técnica e DevOps |
| Lucca | Modelagem do banco Oracle, DER/MER, scripts SQL, procedures, relatórios de banco e aplicativo mobile |
| Rafaela | Protótipo IoT/IA, QA, critérios de qualidade, riscos, métricas, benchmarking e arquitetura TOGAF/Archi |
| Sabelli | API .NET complementar, com foco em Doença, Predisposição e integração com o banco Oracle |

---

## 2. Cronograma resumido

| Período | Atividade principal | Responsável |
| --- | --- | --- |
| 05/05 a 08/05 | Definição da proposta Arkive, revisão do desafio CLYVO VET e alinhamento do PRD | Grupo |
| 08/05 a 10/05 | Modelagem inicial do banco Oracle e definição das entidades principais | Lucca |
| 09/05 a 16/05 | Desenvolvimento da API Java: cadastros, jornada clínica, bem-estar, preventivo, alertas, NPS e eventos de jornada | Gustavo |
| 14/05 a 16/05 | Alinhamento da API Java com o schema Oracle `TB_ARKIVE_*` e validação da conexão com o banco | Gustavo e Lucca |
| 16/05 a 20/05 | Documentação técnica, arquitetura, cronograma, Swagger e collection Postman para testes dos endpoints | Gustavo |
| 20/05 a 24/05 | Revisão final dos artefatos, ajustes de integração, organização do repositório e preparação da entrega | Grupo |

---

## 3. Entregas realizadas na API Java

| Entrega | Status |
| --- | --- |
| Projeto Spring Boot criado e versionado no GitHub | Concluído |
| Estrutura em camadas: Controller, Service, Repository, Entity e DTO | Concluído |
| Persistência em Oracle com JPA/Hibernate | Concluído |
| Endpoints REST para cadastros básicos | Concluído |
| Vínculo histórico entre animal e responsável | Concluído |
| Jornada clínica: consulta, diagnóstico, prescrição e adesão | Concluído |
| Bem-estar, protocolos preventivos, eventos preventivos, alertas e NPS | Concluído |
| Eventos de jornada e timeline do animal | Concluído |
| Swagger/OpenAPI | Concluído |
| Cache simples em recursos de catálogo | Concluído |
| Collection Postman para validação dos endpoints | Concluído |
| Documentação técnica da arquitetura e execução | Concluído |

---

## 4. Observações

A primeira entrega do Arkive foi planejada como um **MVP funcional**, priorizando a implementação da API Java, a persistência no banco Oracle e a validação dos principais fluxos da jornada de saúde do pet.

Funcionalidades mais avançadas, como autenticação completa, dashboards, integração real com WhatsApp, IA em produção e data lakehouse, foram mantidas como evolução futura para não comprometer o escopo da Sprint 1/2.