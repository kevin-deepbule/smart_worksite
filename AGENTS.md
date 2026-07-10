# AGENTS.md

This file describes collaboration rules for the Smart Worksite backend. Read `README.md` first for the project overview, startup steps, and module list.

## Project Positioning

This repository contains the Smart Worksite large-model application. The root-level `src/`, `pom.xml`, and `deploy/` are the Java Spring Boot backend main system. Frontend work must live under `frontend/`.

The target system covers project data, knowledge bases, Q&A, compliance review, report generation, OCR recognition, async tasks, permission security, and audit tracing. Enterprise business capabilities belong in the Java backend; large-model, Agent, RAG, Embedding, OCR algorithm, vector retrieval, and document-parsing capabilities belong in external Python intelligent algorithm services and are integrated by the Java backend through REST/internal service adapters. Frontend must not directly call Python services, databases, MinIO, vector databases, or OCR engines.

## Tech Stack

Backend main system:

- Java 17
- Spring Boot 3.3.x
- Spring Web for REST APIs
- Spring Validation for Bean Validation
- Spring Data Redis for Redis access
- Spring Boot Actuator for health checks and runtime status
- Maven for build and dependency management
- MyBatis + XML for data access
- PageHelper for pagination
- MySQL Connector/J for MySQL access
- Flyway for database migrations
- MinIO Java SDK for object storage
- Apache PDFBox for PDF processing
- Apache POI for Word, Excel, and PPT processing

Frontend application:

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Axios
- Element Plus
- `@element-plus/icons-vue`
- npm

Python intelligent algorithm service:

- Python
- Large-model calls for Q&A, compliance review, and report generation
- Agent orchestration for task decomposition, tool calling, multi-step reasoning, and business workflow coordination
- RAG retrieval augmentation for project knowledge bases, policy/standard libraries, and industry material libraries
- Embedding vectorization for document chunks and semantic retrieval
- OCR recognition for ID cards, license plates, invoices, contracts, and custom fields
- Document parsing for content extraction, layout understanding, and table recognition
- CryptoAgentV3 as the current external Python report-generation integration

Data and infrastructure:

- MySQL 8 for business metadata, permission data, task data, audit logs, and file metadata
- Redis 7 for cache, lightweight queues, distributed locks, and task status acceleration
- MinIO for documents, images, templates, and generated reports
- Docker Compose for local MySQL, Redis, and MinIO
- Milvus or pgvector for planned knowledge-base vector retrieval

Integration boundaries:

- Frontend calls only Java backend REST APIs.
- Java backend owns authentication, authorization, project isolation, business orchestration, status records, file persistence, and audit tracing.
- Java backend integrates Python intelligent algorithm services through REST APIs or internal service interfaces.
- Large models, OCR engines, vector databases, business databases, and object storage are not directly exposed to the frontend. Qwen API keys must be configured only in `python-ai-service/`; Java calls the Python service and must not call Qwen directly for the AI adapter module.
- Cross-service calls must record request summary, response summary, elapsed time, status, and error information.

## Local Dependencies

```powershell
cd deploy
copy .env.example .env
docker compose -f docker-compose-env.yml --env-file .env up -d
```

Docker starts MySQL, Redis, and MinIO only. Business tables are created by Flyway. Do not create business tables through Docker initialization SQL. Vector retrieval components such as Milvus or pgvector are planned separately and are not part of the current local Docker dependency set unless explicitly added.

## Package Structure

Root package: `com.xd.smartworksite`.

Main modules:

- `common`: shared response, exception, request ID, MyBatis config, and Redis helpers.
- `system`: ping, health, version, and runtime status.
- `auth`: users, roles, permissions, login, and project-level access control.
- `project`: worksite projects, members, and project isolation foundation.
- `file`: file metadata, upload/download, parsing records, and MinIO adapter.
- `template`: report/review/common template metadata and file binding.
- `knowledge`: knowledge bases, documents, indexing status, and retrieval-facing metadata.
- `datasource`: business data source configuration and database Q&A foundations.
- `qa`: Q&A sessions, answers, citations, feedback, and model/RAG integration records.
- `review`: compliance templates, review tasks, issue lists, suggestions, and JSON results.
- `report`: report templates, records, versions, downloads, and CryptoAgentV3 integration.
- `ocr`: OCR records, recognition types, structured fields, and result JSON.
- `task`: async tasks, statuses, retries, cancellation, timeouts, and stage logs.
- `audit`: operation audit logs, access logs, model calls, retrieval logs, OCR calls, and external call logs.
- `ai`: Java intelligent capability adapter for Python AI services, including model/Agent calls, RAG search, routing, context preparation, safe database Q&A, and external call logs.

Business modules may use these layers as needed: `controller`, `application`, `domain`, `repository`, `mapper`, `dto`, `infra`.

## Layering Rules

- Controllers handle HTTP input, Bean Validation, request context, and response wrapping only.
- Controllers may call only the module's application service or facade.
- Application services own use-case orchestration and transaction boundaries.
- Domain objects express business concepts, enums, state transitions, and core rules.
- Repositories provide business-facing persistence interfaces. MyBatis implementations should be named `MyBatisXxxRepository`.
- Mappers only handle SQL mapping. Prefer XML for complex SQL.
- Infra contains Redis, MinIO, external HTTP, Python algorithm service, model, OCR, vector retrieval, and other technical adapters.
- Controllers must not directly call mappers, Redis, MinIO, or external services.
- Modules must not directly call another module's mapper.
- Cross-module collaboration should go through the other module's application service or facade.

## Responses And Exceptions

Use `common.result.ApiResponse` for unified responses and `common.result.PageResult` for pagination.

Use `common.exception.BusinessException` for business errors. Global exception handling lives in `common.exception.GlobalExceptionHandler`.

Request IDs are handled by `common.config.RequestIdFilter`. The response header is `X-Request-Id`.

## Database Rules

- Database migrations must use Flyway scripts under `src/main/resources/db/migration`.
- Do not modify migrations that have already been used by the team. Add a new version instead.
- Business tables should include `id`, `created_at`, `updated_at`, `created_by`, `updated_by`, and `deleted`.
- Tables with project-scoped data must include `project_id`.
- SQL should filter `deleted = 0` by default.
- Business data uses logical delete by default.

## Coding Rules

- New APIs must use DTOs and Bean Validation for input validation.
- Request objects are named `XxxRequest` or `XxxCommand`.
- Response objects are named `XxxResponse`; do not return sensitive database fields.
- Do not put business decisions in controllers or MyBatis XML.
- External service calls must define timeout, error mapping, retry/timeout policy where applicable, and call logging.
- AI, RAG, OCR, Embedding, vector retrieval, and document-parsing integrations must be adapter-based; do not implement algorithm core logic in Java controllers or application services.
- Long-running operations such as report generation, OCR recognition, knowledge indexing, and document parsing must use async tasks or status records instead of blocking HTTP requests for the whole job.
- Logs must not print passwords, tokens, MinIO secrets, or production credentials.
- Local development seeds `admin / admin123` through Flyway for interface testing only; production deployments must reset or disable the seeded administrator password.
- Run `mvn test` after adding runnable functionality.

## Agent Rules

- Read `README.md`, this file, and related module code before editing.
- Follow the existing package structure and layer boundaries.
- Keep changes focused and avoid unrelated refactors.
- For every project content change, update `AGENTS.md` and the relevant files under `docs/` in the same change set.
- Documentation updates are required for changes to code, APIs, configuration, technical stack, database schema, module boundaries, startup steps, deployment, or collaboration rules.
- Documentation updates are not required for read-only investigation, temporary debugging commands, log inspection, or other actions that do not change project content.
- If changing public contracts, database schema, external APIs, technical stack decisions, or collaboration rules, update docs as well.
- The workspace may contain user changes; never revert unrelated files.
- Do not write real secrets, accounts, or production addresses into generated SQL, config, or examples.

## Frontend Rules

- Frontend code must be placed in `frontend/`.
- Do not modify backend `src/`, `pom.xml`, or `deploy/` for frontend-only tasks unless explicitly requested.
- Use `npm` for frontend package management.
- Frontend source files containing Chinese text must be saved as UTF-8 and must not contain mojibake or `????` placeholders.
- API base URL must be read from `.env` as `VITE_API_BASE_URL`.
- Development may use mock data when the backend API is not available.
- All HTTP calls must go through `frontend/src/utils/request.ts`.
- Requests should automatically attach `Authorization` when a token exists.
- Requests should automatically attach `X-Request-Id`.
- Handle unified backend responses with `code`, `message`, `data`, `requestId`, and `timestamp`.
- `401` should redirect to `/login`.
- `403` should redirect to `/403`.
- Do not let frontend code directly call Python services, databases, MinIO, vector databases, or OCR engines.
- Reusable upload, table, search, dialog form, status tag, progress, JSON viewer, empty state, and download behavior should be implemented as shared components.
- Every page must handle loading, empty, and error states.
- Long-running tasks such as report generation, OCR recognition, and knowledge indexing must show status, progress, or stage logs.
- AI results should expose traceable information where available, such as sources, confidence, raw JSON, or document references.

## Frontend UI Style

- Follow `智慧工地前端UI风格指南.md`.
- Use an enterprise smart worksite admin style with a lightweight data cockpit feel.
- Prefer a light theme.
- Use industrial blue as the primary color, with teal and construction orange as accent colors.
- Use a left-side menu, top project switcher, and card-based content sections.
- Do not use a pure big-screen dashboard style.
- Do not use dark mode as the default.
- Do not use a flashy consumer AI chat product style.



## Python Intelligent Service Rules

- The Python intelligent algorithm service lives under `python-ai-service/`.
- Qwen API keys must stay in the Python service `.env` or environment variables and must not be written to Java config, docs, SQL, or logs.
- Java backend calls Python through `app.ai.python-service.*` and sends `X-AI-Service-Key` when configured.
- Database Q&A uses Python to generate SQL and summaries, but Java must validate and execute only safe MySQL read-only SQL.
- RAG, Agent, model reasoning, context compression, and semantic routing are Python responsibilities; Java owns project isolation, logging, error mapping, and API responses.


## Advanced AI Adapter Rules

- RAG indexing must use the Python service: document chunking, embedding, vector storage, retrieval, and rerank belong in `python-ai-service/`.
- Supported vector providers are `LOCAL`, `PGVECTOR`, and `MILVUS`; Java must not directly access vector databases.
- `EMBEDDING_PROVIDER=QWEN` is the production path; `LOCAL_HASH` is only for offline tests and development without model quota.
- Agent tool execution is coordinated by Python through a tool registry; Java exposes stable APIs and logs calls.
- Database Q&A supports MySQL, PostgreSQL, and Kingbase through JDBC, but still only permits read-only `SELECT`/`WITH` statements.

- PostgreSQL and Kingbase database Q&A execution requires JDBC drivers and the same read-only SQL safety checks as MySQL. Real Kingbase execution tests use `AI_TEST_KINGBASE_JDBC_URL`, `AI_TEST_KINGBASE_USERNAME`, and `AI_TEST_KINGBASE_PASSWORD`; do not fake production credentials in repository files.
