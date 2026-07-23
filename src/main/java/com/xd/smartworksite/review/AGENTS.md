# Review Module Design Rules

本文件补充根目录 `AGENTS.md`，约束合规审查的文件解析、长文档切分、规则执行、结果汇总、异步任务和持久化设计。发生冲突时，以根目录规则和用户当前明确要求为准。

## Scope And Boundaries

- `review` 模块负责审查记录、规则快照、审核编排、规则级结果、证据、整改建议、进度、重试和归档。
- 被审核文件的上传、格式识别、解析记录、Markdown 结果保存和解析重试必须复用 `file` 模块。
- `review` 模块只能通过 `FileObjectApplicationService`、`FileParseApplicationService` 或文件模块提供的 Facade 使用文件能力，不得直接访问文件 Mapper、MinIO、PDFBox、POI、OCR 或解析模型。
- `review` 模块不得直接调用 Qwen、Python 文档解析接口或其他模型提供方；模型审核必须通过 Java `ai` 适配层调用 `python-ai-service`。
- `task` 模块负责 `REVIEW_EXECUTION` 异步任务、outbox 投递、Worker claim、租约、心跳、取消和阶段日志。
- Java 负责项目隔离、权限、规则快照、任务状态、结果持久化、枚举校验和总体状态计算；Python 负责文档解析算法、长文本处理建议、模型审核和自然语言汇总。

## Reviewed File Contract

- 审核文件第一阶段支持 `DOC`、`DOCX`、`PDF`、`TXT`、`XLS`、`XLSX`。
- PDF 扫描件是否需要 OCR 由文件解析服务判断；无法解析、加密或内容为空必须明确失败。
- TXT 必须处理受支持编码，无法可靠解码时明确失败，不能用乱码继续审核。
- Excel 必须保留 Sheet 名、表头和行号；超大 Sheet 按行分块时每块重复表头。
- 审核文件必须是当前项目的有效文件，项目必须可写。
- 不支持的格式、超限页数、超限 Sheet/行数、损坏文件和宏文件必须返回可见错误。

## File Parse Reuse

- 提交审查后，调用 `FileParseApplicationService` 创建或复用 `targetFormat = MARKDOWN` 的解析记录。
- `file_parse_record` 是文件解析状态和结果的事实来源；`review_record` 保存关联的 `parse_record_id`。
- 解析成功后，通过文件模块的 system-safe 方法读取 Markdown，不在 review 中读取 MinIO。
- 解析结果为空、格式不是 `MARKDOWN`、对象不存在或读取失败时，审核任务失败。
- 审核不得使用被静默截断的解析结果。解析元数据出现 `inputTruncated = true` 时必须失败；当前 file 模块对超过单次模型窗口的文本执行全量分段解析并按源顺序合并。
- 相同文件 Hash 和目标格式已有可复用成功解析记录时优先复用，不重复调用解析服务。
- 异步 Worker 不读取请求线程 `SecurityContext`；文件模块需要提供 system-safe 创建解析、查询状态和读取内容能力，并重新校验项目及文件归属。
- 不允许 Review Worker 使用循环加长时间休眠阻塞等待解析。优先使用 file parse 完成事件/outbox 继续审核；未具备事件时可由短周期调度器推进处于 `PARSING` 的记录。

## Review Template And Rule Snapshot

- 模板必须属于审查项目、类别为 `REVIEW`、状态为 `ENABLED`。
- 审查模板规则来自 `{{ var_xxx : 审核规则描述 }}`，变量名作为稳定的 `ruleCode`，当前持久化描述作为规则内容。
- 提交审查时必须按模板文件顺序快照全部规则，保存 `ruleCode`、规则描述、顺序、模板 ID 和模板文件 ID。
- 模板没有规则、存在空规则描述或规则无法读回时，不得创建可执行审核任务。
- 审核开始后修改模板规则只影响后续新审查，不得改变历史审查的规则快照。
- 每个规则最终必须产生且只产生一条规则级结果。

## Markdown Chunking

- 完整 Markdown 不能直接一次性发送给模型；超过单次模型安全上下文时必须切分。
- 优先按 Markdown 标题层级切分，再按段落、列表和表格切分，最后才按 Token 上限切分。
- 表格尽量整体保留；超大表格按行切分并重复表头。
- 建议默认块大小为 `1200~2000 tokens`，重叠比例为 `10%~15%`，具体值配置化。
- 每个块必须有稳定 `chunkCode`、顺序、章节路径、页码范围、Sheet/行号范围、Token 数和内容 Hash。
- PDF Markdown 应保留页码锚点；Excel Markdown 应保留 Sheet 和行号锚点。
- 块内容建议保存到 MinIO，MySQL 保存块元数据和对象引用；不得把完整正文写入普通日志或外部调用摘要。
- 切块必须检查最大块数、最大总 Token 和最大调用次数，超限时明确失败，不能只审核前半部分。

## Three-Level Review Pipeline

### 1. Chunk Evidence Evaluation

- 块级调用的职责只是判断“当前文档块是否包含与规则有关的证据”，不能直接代表整篇文档的最终结论。
- 实际调用可使用“一个文档块 + 5~10 条规则”的批次降低成本，但模型结果必须按规则逐条返回。
- 块级内部观察值只允许：
  - `SUPPORTING_EVIDENCE`：发现支持规则的证据。
  - `VIOLATING_EVIDENCE`：发现明确违反或冲突证据。
  - `PARTIAL_EVIDENCE`：只发现规则部分要求。
  - `NO_EVIDENCE`：当前块没有相关内容。
  - `NOT_APPLICABLE`：当前块与规则无关。
- 当前块没有内容只能返回 `NO_EVIDENCE` 或 `NOT_APPLICABLE`，不得直接判定整篇文档不符合。
- 块级结果应包含 `matchedRequirements`、`missingRequirements`、原因、证据、位置和置信度。
- 块级观察是规则汇总过程中的中间数据，不建立 `review_rule_chunk_result` 全量关系表。`NO_EVIDENCE`、`NOT_APPLICABLE` 等无有效证据结果只在本次规则执行上下文中使用，不写入 MySQL。
- 只有支撑最终结论的有效证据才写入 `review_rule_result.evidence_json`，并引用对应的 `review_document_chunk.id`。

### 2. Per-Rule Reduction

- 某条规则的全部相关块完成后，才能汇总为规则最终状态。
- 规则汇总输入必须包含全部观察计数和完整的 matched/missing requirements；为控制上下文，仅按证据类型抽取有上限的证据样本给汇总模型，完整有效证据仍保存到 `evidence_json`。
- 规则最终状态只允许：
  - `COMPLIANT`：符合。
  - `NON_COMPLIANT`：不符合。
  - `PARTIALLY_COMPLIANT`：部分符合。
- `COMPLIANT` 表示规则的全部要求都有明确支持证据且未发现冲突。
- `NON_COMPLIANT` 表示发现明确违反、必填内容完全缺失，或扫描完整文档后仍无任何必要证据。
- `PARTIALLY_COMPLIANT` 表示只满足部分要求、信息不完整、证据不足或不同位置存在矛盾。
- `NON_COMPLIANT` 和 `PARTIALLY_COMPLIANT` 的 `reason`、`suggestion` 必须非空。
- 对“全文缺失”类不符合结果允许没有原文引文，但原因必须说明已检查的范围。
- 规则级证据必须引用块 ID 及页码、章节或 Sheet/行号；引文应能在对应块内容中找到。

### 3. Final Summary

- 最终汇总模型只接收规则最终结果，不再接收完整 Markdown。
- Java 按确定性规则计算总体状态：
  - 只要存在 `NON_COMPLIANT`，总体为 `NON_COMPLIANT`。
  - 否则只要存在 `PARTIALLY_COMPLIANT`，总体为 `PARTIALLY_COMPLIANT`。
  - 否则总体为 `COMPLIANT`。
- 汇总模型只生成摘要、统计、重点风险和综合建议，不得修改任何规则状态或 Java 计算的总体状态。
- 汇总结果必须包括规则总数、符合数、部分符合数和不符合数。

## Model Output Validation

- 所有模型结果必须是严格 JSON，Java 必须校验 JSON 结构、枚举、必填字段、规则集合和重复项。
- 模型返回未知规则、重复规则、缺少规则、未知状态或无效证据结构时，该次调用失败。
- `NON_COMPLIANT`、`PARTIALLY_COMPLIANT` 缺少原因或建议时，该次规则汇总失败。
- 空结果、非 JSON、结果截断和外部服务失败不得生成默认“符合”结果。
- 可对格式错误执行一次受控的 JSON 修正重试；仍然失败时记录原始错误并将对应任务或规则标为失败。
- 证据引文应校验确实存在于对应 Markdown 块；无法定位的证据不得作为确定性依据。

## Async Task And State

- 提交审核接口必须快速返回 `PENDING` 记录和 `taskId`，不能等待解析和全部模型调用完成。
- 审核使用 `REVIEW_EXECUTION` 任务。文件解析期间任务保持 `PENDING`；解析成功且确认 `inputTruncated=false` 后，解析协调器才将任务改为 `QUEUED` 并通过 `task_outbox` 可靠投递。
- `review_record.status` 使用粗粒度状态：`PENDING`、`PROCESSING`、`COMPLETED`、`FAILED`、`CANCELED`、`ARCHIVED`。
- `current_stage` 使用：`PARSING`、`QUEUED`、`CHUNKING`、`REVIEWING`、`REDUCING_RULES`、`SUMMARIZING`、`FINISHED`、`FAILED`、`CANCELED`。
- 建议阶段日志：`REVIEW_VALIDATE`、`REVIEW_PARSE_SUBMIT`、`REVIEW_PARSE_WAIT`、`REVIEW_CHUNK`、`REVIEW_CHUNK_EVALUATE`、`REVIEW_RULE_REDUCE`、`REVIEW_FINAL_SUMMARY`、`REVIEW_PERSIST_RESULT`。
- Worker claim、完成和失败必须带 owner 校验；每个文档块模型调用前必须续租任务租约。
- Worker 必须在阶段边界和每个文档块调用前检查 `cancel_requested`，取消后停止后续模型调用并落为 `CANCELED`。
- 状态、阶段日志、规则结果、文档块和外部调用日志写入都必须检查影响行数或生成 ID。

## Persistence

- 数据库变更必须新增 Flyway 迁移，不修改既有迁移。
- 审核持久化采用 `review_record`、`review_document_chunk`、`review_rule_result` 三表结构，不建立“规则数 × 文档块数”的全量中间结果表。
- 完整解析 Markdown 和各块正文保存到 MinIO；MySQL 只保存业务状态、块元数据、规则最终结果及有效证据引用。
- 不建立数据库外键，跨表归属、项目一致性和逻辑删除状态由应用层校验。
- `issues_json` 和 `result_json` 可继续作为兼容汇总字段，但不能替代 `review_rule_result` 这一规则级事实来源。

### `review_record`

沿用现有审核主表，并通过新 Flyway 迁移增加以下字段：

| 字段 | 类型 | 约束与含义 |
| --- | --- | --- |
| `parse_record_id` | `BIGINT` | 可空，关联 file 模块的 Markdown 解析记录 |
| `current_stage` | `VARCHAR(32)` | `PARSING/QUEUED/CHUNKING/REVIEWING/REDUCING_RULES/SUMMARIZING/FINISHED/FAILED/CANCELED` |
| `progress` | `INT` | 非空，默认 `0`，范围 `0~100` |
| `overall_status` | `VARCHAR(32)` | 完成前可空；只允许 `COMPLIANT/NON_COMPLIANT/PARTIALLY_COMPLIANT` |
| `summary` | `MEDIUMTEXT` | 最终摘要、重点风险和综合建议 |
| `rule_total` | `INT` | 非空，规则总数 |
| `rule_completed` | `INT` | 非空，已完成规则数 |
| `chunk_total` | `INT` | 非空，Markdown 文档块总数 |
| `started_at` | `DATETIME` | Worker 实际开始处理时间 |
| `completed_at` | `DATETIME` | 审核进入终态的时间 |

- `status` 继续表示粗粒度执行状态，`overall_status` 只表示业务合规结论，两者不得混用。
- `parse_record_id`、`overall_status` 应分别建立普通索引。
- 当前 `issues_json` 可由 `NON_COMPLIANT`、`PARTIALLY_COMPLIANT` 规则结果派生；`result_json` 可保存接口兼容快照。

### `review_document_chunk`

一条记录表示被审核 Markdown 的一个稳定文档块：

| 字段 | 类型 | 约束与含义 |
| --- | --- | --- |
| `id` | `BIGINT` | 自增主键 |
| `project_id` | `BIGINT` | 非空，项目 ID |
| `review_record_id` | `BIGINT` | 非空，审核记录 ID |
| `parse_record_id` | `BIGINT` | 非空，file 模块解析记录 ID |
| `chunk_no` | `INT` | 非空，从 `1` 开始的块顺序 |
| `chunk_code` | `VARCHAR(64)` | 非空，稳定编码，例如 `CHUNK_0001` |
| `heading_path` | `VARCHAR(1000)` | 可空，Markdown 标题路径 |
| `page_start/page_end` | `INT` | 可空，Word/PDF 页码范围 |
| `sheet_name` | `VARCHAR(255)` | 可空，Excel Sheet 名称 |
| `row_start/row_end` | `INT` | 可空，Excel 行号范围 |
| `content_object_name` | `VARCHAR(500)` | 非空，块正文在 MinIO 中的对象名 |
| `content_hash` | `VARCHAR(128)` | 非空，块正文 SHA-256 |
| `char_count` | `INT` | 非空，默认 `0` |
| `token_count` | `INT` | 可空，模型 Token 估算值 |
| `status` | `VARCHAR(32)` | 非空，默认 `READY`；只允许 `READY/FAILED` |
| `error_message` | `TEXT` | 可空，分块失败原因 |
| 审计字段 | 标准字段 | `created_at/updated_at/created_by/updated_by/deleted` |

- 建立唯一键 `review_record_id + chunk_no + deleted`，保证队列重投不会重复生成同序号文档块。
- 建立 `project_id`、`review_record_id`、`parse_record_id`、`content_hash` 和 `deleted` 索引。
- Word/PDF 优先填写标题和页码定位；Excel 填写 Sheet 和行号；TXT 无结构定位时至少保留 `chunk_no`。
- `content_object_name` 是内部对象引用，不能通过列表和详情接口直接暴露给前端。

### `review_rule_result`

一条记录表示一次审核中一条规则的快照、执行状态和最终结果：

| 字段 | 类型 | 约束与含义 |
| --- | --- | --- |
| `id` | `BIGINT` | 自增主键 |
| `project_id` | `BIGINT` | 非空，项目 ID |
| `review_record_id` | `BIGINT` | 非空，审核记录 ID |
| `template_id` | `BIGINT` | 非空，提交时的模板 ID 快照 |
| `template_file_id` | `BIGINT` | 可空，提交时的模板文件 ID 快照 |
| `rule_code` | `VARCHAR(128)` | 非空，模板变量名 |
| `rule_description` | `TEXT` | 非空，提交时的规则描述快照 |
| `rule_order` | `INT` | 非空，默认 `0` |
| `execution_status` | `VARCHAR(32)` | 非空；`PENDING/RUNNING/SUCCESS/FAILED` |
| `compliance_status` | `VARCHAR(32)` | 完成前可空；三态最终结论 |
| `reason` | `TEXT` | 可空；不符合或部分符合时必须非空 |
| `suggestion` | `TEXT` | 可空；不符合或部分符合时必须非空 |
| `evidence_json` | `JSON` | 可空，只保存支撑最终结论的有效证据 |
| `confidence` | `DECIMAL(5,4)` | 可空，范围 `0~1` |
| `model_trace_id` | `VARCHAR(128)` | 可空，Python/模型调用追踪 ID |
| `error_message` | `TEXT` | 可空，规则审核失败原因 |
| `started_at/completed_at` | `DATETIME` | 可空，规则执行起止时间 |
| 审计字段 | 标准字段 | `created_at/updated_at/created_by/updated_by/deleted` |

- 建立唯一键 `review_record_id + rule_code + deleted`，保证同一次审核每条规则只有一个最终结果。
- 建立 `project_id`、`review_record_id`、`execution_status`、`compliance_status` 和 `deleted` 索引。
- 规则创建时立即写入 `rule_code`、`rule_description`、`rule_order`、模板 ID 和模板文件 ID 快照，不能等模型执行时再读取活动模板。
- `execution_status = SUCCESS` 时才允许写最终 `compliance_status`；`execution_status = FAILED` 时必须写 `error_message`。

### `evidence_json`

`evidence_json` 是有效证据数组，每个元素使用以下结构：

```json
[
  {
    "chunkId": 1024,
    "chunkCode": "CHUNK_0008",
    "location": {
      "headingPath": "第三章/高处作业",
      "pageStart": 12,
      "pageEnd": 13,
      "sheetName": null,
      "rowStart": null,
      "rowEnd": null
    },
    "evidenceType": "PARTIAL_EVIDENCE",
    "quote": "高处作业人员应正确佩戴安全带。",
    "analysis": "文档规定了安全带要求，但没有规定检查频率和责任人。"
  }
]
```

- `chunkId` 必须引用同一个 `review_record_id` 下未删除的 `review_document_chunk`。
- `evidenceType` 只允许 `SUPPORTING_EVIDENCE`、`VIOLATING_EVIDENCE`、`PARTIAL_EVIDENCE`。
- `quote` 必须能在对应块正文中定位并设置最大长度，不能把整块 Markdown 复制进 JSON。
- `NO_EVIDENCE` 和 `NOT_APPLICABLE` 不保存到 `evidence_json`。
- JSON 必须先由应用层完成结构、枚举、块归属和文本长度校验，再以 MyBatis 参数写入；Mapper 不使用 `CAST(? AS JSON)`。

## Retry And Idempotency

- 失败重试必须按阶段恢复，不得默认从头重新解析和审核。
- 文件解析已成功时复用 Markdown；切块已成功时复用块。
- 已经 `SUCCESS` 的 `review_rule_result` 不重复生成。
- 因为不持久化全量块级观察，一条规则在所有块处理完成前失败时，以该规则为最小重试单位，重新执行该规则的全部文档块；不能宣称从失败块精确续跑。
- 规则汇总失败时，由于不保存全量块级观察，按整条规则重新执行全部文档块；最终总结失败时复用成功规则结果并只重试最终总结。
- 文档块使用 `content_hash` 保证稳定性；规则输入未变化时可复用已经成功的规则最终结果。
- 重试次数和模型调用总次数必须配置上限，超过上限后明确失败。
- `REVIEW_MAX_MODEL_CALLS` 按包含一次 JSON 修正重试的最坏情况估算本次审核调用数，超限时在开始逐块调用前失败。

## API And Access Rules

- 项目级查询、提交、重试、取消、归档和问题处理必须执行项目隔离。
- 非平台管理员跨项目查询必须限制在当前用户可访问项目集合。
- 规则结果接口返回规则快照、三态结果、原因、建议、证据、置信度和错误。
- 审查详情返回进度、阶段、规则/块完成数和总体汇总，不默认返回完整 Markdown 正文。
- 查看解析正文或块内容应使用单独受控接口和权限，不得在列表接口返回。
- 前端必须轮询非终态记录，并展示解析、切块、规则审核和汇总阶段进度。

## Observability And Security

- 文档解析、块级审核、规则汇总和最终汇总都必须写外部调用日志。
- 外部调用日志记录服务、调用类型、请求摘要、响应摘要、耗时、状态和错误，不保存完整 Markdown、文件临时 URL、Token 或密钥。
- 每个规则结果应保留模型 trace ID，便于定位具体调用。
- 日志不得打印文档正文、证据全文、图片 base64、MinIO 地址或生产凭据。
- 解析失败、模型失败、持久化失败和取消必须有明确阶段及错误原因。

## Required Tests

- Word、PDF、TXT、XLS、XLSX 解析为完整 Markdown。
- 扫描 PDF、空文件、加密 PDF、损坏 Office、编码异常 TXT 和超大 Excel 明确失败。
- 解析结果截断时禁止进入审核。
- Markdown 标题、段落、长表格、页码和 Sheet/行号切分。
- 当前块无证据不会直接产生规则级不符合。
- 全文无必要证据最终产生 `NON_COMPLIANT`。
- 部分要求缺失最终产生 `PARTIALLY_COMPLIANT`，且原因和建议非空。
- 全部要求满足产生 `COMPLIANT`。
- 模型返回非法 JSON、未知枚举、重复/缺失规则和无效证据时失败。
- 规则快照不受后续模板修改影响。
- 任务重投不会重复生成文档块或规则结果，阶段重试复用成功规则，失败规则按整条规则重跑，取消停止后续调用。
- 项目隔离、禁用项目、跨项目模板/文件和无权限访问失败。
- 任务、规则、块、阶段日志和外部调用日志写入零行时失败。
