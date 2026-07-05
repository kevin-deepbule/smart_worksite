# File Module Agent Guide

This document describes the intended design for the `file` module. Read the root `README.md` and root `AGENTS.md` before editing this module.

## Module Positioning

The `file` module provides unified file object management for the Smart Worksite backend.

It must support upload, download URL generation, preview URL generation, logical deletion, physical object deletion, and metadata persistence for documents, images, templates, reports, OCR attachments, and other business files.

Use this module as the single backend entry point for file metadata and MinIO object operations. Other modules such as `knowledge`, `report`, `ocr`, `review`, and `qa` should reference files by `file_id` or call this module through its application service or facade. They must not call MinIO or this module's mapper directly.

## Storage Model

Use MySQL for file metadata and MinIO for binary object storage.

- MySQL table: `file_object`
- Object storage adapter: `file.infra.StorageAdapter`
- MinIO implementation: `file.infra.MinioStorageAdapter`
- Configuration prefix: `app.storage.minio`
- Default signed URL expiration: `app.file.access-url-expire-seconds`

The object content is stored only in MinIO. The database stores object identity, business ownership, project isolation fields, status, and searchable metadata.

## Recommended Package Structure

When implementing the module, use these layers:

```text
com.xd.smartworksite.file
├── controller
│   └── FileObjectController
├── application
│   └── FileObjectApplicationService
├── domain
│   ├── FileObject
│   ├── FileBizType
│   └── FileStatus
├── dto
│   ├── FileUploadRequest
│   ├── FileQueryRequest
│   ├── FileObjectResponse
│   └── FileAccessUrlResponse
├── repository
│   ├── FileObjectRepository
│   └── MyBatisFileObjectRepository
├── mapper
│   └── FileObjectMapper
└── infra
    ├── StorageAdapter
    ├── StorageObject
    ├── MinioStorageAdapter
    └── MinioStorageProperties
```

Follow the project layering rules:

- Controllers handle HTTP input, Bean Validation, and `ApiResponse` wrapping only.
- Controllers call only `FileObjectApplicationService` or a future file facade.
- Application services own business orchestration and transaction boundaries.
- Repositories expose business-facing persistence methods.
- Mappers only execute SQL and must filter `deleted = 0` by default.
- Infra code encapsulates MinIO SDK details.

## Business Types

Use a domain enum for `biz_type`. Recommended values:

```text
DOCUMENT    Project documents, standards, PDF, Word, Excel, and uploaded knowledge files
IMAGE       Site photos, inspection images, and visual evidence
TEMPLATE    Report templates, review templates, and reusable document templates
REPORT      Generated reports and exported result files
OCR         OCR source files or OCR result attachments
OTHER       Other attachments
```

`biz_id` is optional during upload. It can be null for temporary or unbound files, then associated later by a business use case.

## File Status

Use a domain enum for `status`. Recommended values:

```text
ACTIVE          File is available
DELETED         File has been logically deleted
DELETE_PENDING  Metadata is deleted or hidden, but object deletion needs retry
```

Do not expose physically deleted MinIO object names as accessible URLs.

## Metadata Table

The current `file_object` table already contains the core fields:

```text
id
project_id
biz_type
biz_id
file_name
object_name
content_type
file_size
file_hash
status
metadata
created_at
updated_at
created_by
updated_by
deleted
```

Short-term implementation may reuse this table directly.

If more structured file metadata is required later, add a new Flyway migration instead of changing used migrations. Suggested optional fields:

```sql
ALTER TABLE file_object
  ADD COLUMN file_ext VARCHAR(32) NULL COMMENT 'File extension' AFTER file_name,
  ADD COLUMN storage_bucket VARCHAR(128) NULL COMMENT 'Storage bucket' AFTER object_name,
  ADD COLUMN preview_supported TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether preview is supported' AFTER metadata,
  ADD KEY idx_file_hash (file_hash),
  ADD KEY idx_file_created_at (created_at);
```

Do not store secrets, MinIO credentials, access keys, or permanent public URLs in `metadata`.

## Object Naming

Never use the original file name directly as the MinIO object name. Generate an object key that avoids collisions and does not leak business-sensitive information.

Recommended pattern:

```text
projects/{projectId}/{bizType}/{yyyy}/{MM}/{dd}/{uuid}.{ext}
```

Example:

```text
projects/1001/DOCUMENT/2026/07/05/9b4f1c7c0a8d4a9c.pdf
```

Store the original uploaded name in `file_name`.

## REST API Contract

Recommended endpoints:

```text
POST   /api/files/upload
GET    /api/files
GET    /api/files/{fileId}
GET    /api/files/{fileId}/download-url
GET    /api/files/{fileId}/preview-url
DELETE /api/files/{fileId}
```

Upload request should use `multipart/form-data`:

```text
file       MultipartFile, required
projectId  Project ID, required
bizType    DOCUMENT / IMAGE / TEMPLATE / REPORT / OCR / OTHER, required
bizId      Business ID, optional
metadata   JSON string, optional
```

Responses must use `common.result.ApiResponse`.

Paginated list responses must use `common.result.PageResult`.

Do not return MinIO credentials, internal bucket configuration, or sensitive object storage details.

## Upload Flow

The upload use case should be implemented in `FileObjectApplicationService`:

1. Validate project ID, business type, file presence, file size, file extension, and content type.
2. Calculate a stable file hash, preferably SHA-256.
3. Generate the MinIO object name.
4. Upload the binary stream through `StorageAdapter.upload(...)`.
5. Persist a `file_object` metadata row with `status = ACTIVE` and `deleted = 0`.
6. Return `FileObjectResponse`.

MinIO upload and MySQL insert are not in the same transaction. Prefer this consistency strategy:

- Upload MinIO object first.
- Insert MySQL metadata second.
- If metadata insert fails, try to delete the just-uploaded MinIO object and log the cleanup result.
- Do not log file content, credentials, tokens, or production endpoints.

## Download And Preview URL Flow

Use signed URLs for download and preview access.

The current adapter method is:

```java
String createAccessUrl(String objectName, Duration expire);
```

For basic implementation, both download and preview endpoints may return a signed GET URL with an expiration time.

If download file names or response headers are needed later, extend the adapter with an overload that accepts response headers instead of building MinIO SDK logic in the controller.

Preview support should be conservative:

```text
Images: image/png, image/jpeg, image/webp
PDF: application/pdf
Text: text/plain
Office documents: do not convert in this module during the foundation phase
```

For unsupported preview types, throw `BusinessException` with `ErrorCode.PARAM_ERROR` or return a response that clearly marks preview as unsupported, depending on the API style chosen for the implementation.

## Delete Flow

Default behavior should be logical delete plus object cleanup.

Recommended simple flow:

1. Find the file by `fileId` and verify `deleted = 0`.
2. Verify project isolation and access permission when auth is available.
3. Delete the MinIO object through `StorageAdapter.delete(objectName)`.
4. Mark metadata as `deleted = 1` and `status = DELETED`.

If object deletion fails and asynchronous compensation is introduced later:

- Mark `status = DELETE_PENDING`.
- Hide the file from normal queries.
- Retry physical deletion through a task or scheduled compensation job.

Do not hard-delete metadata rows by default.

## Query Rules

All metadata queries must filter:

```sql
deleted = 0
```

Project-scoped queries must also filter:

```sql
project_id = #{projectId}
```

Common query filters:

- `projectId`
- `bizType`
- `bizId`
- `status`
- keyword matching `file_name`
- created time range

Default sort:

```sql
order by created_at desc, id desc
```

## Permission And Project Isolation

Every public API must enforce project isolation.

When the auth/project permission foundation is not complete, keep a clear application-service-level validation point:

```java
// TODO: verify current user has access to projectId before file operation.
```

Do not implement project access checks in MyBatis XML as hidden business logic. XML should only apply explicit query filters.

## Metadata JSON

Use `metadata` for business-specific extension data that does not need first-class columns.

Example:

```json
{
  "source": "manual_upload",
  "scene": "knowledge_import",
  "width": 1920,
  "height": 1080,
  "templateType": "monthly_report",
  "remark": "safety inspection photo"
}
```

Keep common searchable fields as table columns, not JSON-only fields.

## Error Handling

Use `common.exception.BusinessException` for business errors.

Suggested mappings:

- Missing file, invalid business type, unsupported preview type: `ErrorCode.PARAM_ERROR`
- File not found or logically deleted: `ErrorCode.NOT_FOUND`
- Duplicate or conflicting state if introduced: `ErrorCode.CONFLICT`
- MinIO operation failure: `ErrorCode.EXTERNAL_SERVICE_ERROR`
- Unexpected persistence failure: let global exception handling map it as a system error

MinIO SDK exceptions should be wrapped in the infra adapter or converted by the application service. Do not leak SDK exception messages directly to API consumers.

## Configuration

Recommended future file-specific configuration:

```yaml
app:
  file:
    access-url-expire-seconds: 600
    max-size-bytes: 104857600
    allowed-content-types:
      - application/pdf
      - image/png
      - image/jpeg
      - image/webp
      - text/plain
```

Use environment variables for deployment-specific values. Do not write real secrets into config, SQL, examples, or tests.

## Testing Guidance

After adding runnable file-management functionality, run:

```bash
mvn test
```

Add focused tests around:

- object name generation
- business type and content type validation
- upload metadata persistence
- signed URL generation flow
- logical delete behavior
- repository queries filtering `deleted = 0`

For MinIO integration, prefer adapter-level tests with a controlled test container or mockable adapter boundary. Do not require real production MinIO in unit tests.
