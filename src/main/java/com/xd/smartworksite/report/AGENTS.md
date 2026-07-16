# Report Module Design Rules

This file supplements the root `AGENTS.md` for knowledge-based report generation.

## Creation Contract

- `POST /api/reports` requires `projectId`, `reportName`, `reportType`, `templateId`, and one `knowledgeBaseId`.
- The knowledge base must exist, belong to the report project, and be `ENABLED`.
- The template must be an enabled project-owned `REPORT` DOCX template.
- Creation reads the template module's ordered `{{ var_xx_xx }}` variables and descriptions. Templates without variables or with any blank description fail before report/task/outbox persistence succeeds.
- A `report_variable_value` row is created for every unique ordered variable. Variable name, description, template/file IDs, knowledge base, task, and creator are immutable generation snapshots.

## Worker Contract

- Report creation remains asynchronous. The Worker must claim the `REPORT_GENERATION` task and re-check project writability.
- Variables run sequentially by `sort_no`. Each call receives only report metadata, one variable name/description, and one knowledge base; conversation context is always empty.
- Report generation reuses the QA/RAG application gateway through a system-safe application service. It does not create `qa_session` or `qa_message` rows and must not call the Java application's own HTTP controllers.
- Worker-side RAG retrieval and model generation must call `searchKnowledgeForSystem` and `invokeModelForSystem`. These paths validate project existence/writability without depending on a logged-in request `SecurityContext`; user-facing QA continues to use the normal access-checked methods.
- Empty RAG results are allowed to continue to the model. The prompt must forbid fabricated concrete project data when only general model knowledge is available.
- Each variable is persisted immediately. `RUNNING`, `SUCCESS`, and `FAILED` updates must check affected rows and retain provider trace and retrieval references when available.
- A variable failure marks the whole report task failed but preserves prior successes. Retrying the same task skips non-blank `SUCCESS` variables and regenerates only `PENDING` or `FAILED` rows.
- DOCX rendering starts only after all variables have non-blank successful values. Body, table, header, and footer placeholders use the same generated value for repeated variable names.

## Query And Frontend Contract

- `GET /api/reports/{reportId}/variables` returns ordered variable name, description snapshot, value, status, trace, timing, and error fields after project access validation.
- The frontend creation dialog contains one enabled knowledge-base selector and no report reference-file or source-type controls.
- The report detail page polls non-terminal reports and displays per-variable progress and failures.

## Persistence

- `V18__add_report_variable_values.sql` creates `report_variable_value`; do not modify V17 or V18 after team use.
- The unique key is `report_id + variable_name`; a repeated placeholder is generated once per report.
- JSON references are application-serialized and passed as normal MyBatis parameters without mapper-level casts.

## Verification

- Backend tests cover ordered snapshots, blank-description rejection, per-variable generation, failure persistence, retry resume, knowledge-base validation, empty-RAG model fallback, and DOCX rendering.
- Run `mvn clean test` and frontend `npm run build` after contract changes.
