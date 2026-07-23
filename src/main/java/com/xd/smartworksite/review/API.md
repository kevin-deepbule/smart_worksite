# 合规审查模块接口文档

本文档描述 `review` 模块当前已经实现的长文档异步审核接口和执行契约。

所有接口都需要：

```http
Authorization: Bearer <accessToken>
```

权限要求：

| 权限 | 说明 |
| --- | --- |
| `review:view` | 查询审查记录 |
| `review:manage` | 提交、重试、取消、删除、归档和更新问题 |

统一响应结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "requestId": "xxx",
  "timestamp": "2026-07-11T16:00:00+08:00"
}
```

## 接口总览

| 方法 | 路径 | 用途 | 状态 |
| --- | --- | --- | --- |
| `POST` | `/api/review/records` | 上传文件并创建异步审查 | 已实现 |
| `GET` | `/api/review/records` | 分页查询审查记录 | 已实现 |
| `GET` | `/api/review/records/{recordId}` | 查询状态、进度和汇总详情 | 已实现 |
| `GET` | `/api/review/records/{recordId}/rule-results` | 查询全部规则审核结果 | 已实现 |
| `GET` | `/api/review/records/{recordId}/rule-results/{ruleCode}` | 查询单条规则结果及证据 | 已实现 |
| `GET` | `/api/review/records/{recordId}/stages` | 查询审核阶段日志 | 已实现，复用任务阶段日志 |
| `POST` | `/api/review/records/{recordId}/retry` | 按失败阶段重试 | 已实现 |
| `POST` | `/api/review/records/{recordId}/cancel` | 取消非终态审核 | 已实现 |
| `DELETE` | `/api/review/records/{recordId}` | 删除审查记录 | 已实现 |
| `POST` | `/api/review/records/{recordId}/archive` | 归档审查记录 | 已实现 |
| `PUT` | `/api/review/records/{recordId}/issues/{issueId}` | 更新审查问题处理状态 | 已实现 |

## 1. 提交审查

```text
POST /api/review/records
Content-Type: multipart/form-data
```

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| projectId | Long | 是 | 项目 ID |
| templateId | Long | 是 | 审查模板 ID |
| file | MultipartFile | 是 | 审查文件，支持 DOC、DOCX、PDF、TXT、XLS、XLSX |

示例：

```bash
curl --noproxy '*' -X POST "http://127.0.0.1:8080/api/review/records" \
  -H "Authorization: Bearer $TOKEN" \
  -F "projectId=1" \
  -F "templateId=1" \
  -F "file=@/tmp/contract.docx"
```

执行行为：

1. 校验项目和启用状态的 REVIEW 模板。
2. 上传文件，复用 file 模块创建或复用 `MARKDOWN` 解析记录。
3. 在一个数据库事务中创建 `review_record`、快照全部审核规则并创建 `PENDING` 的 `REVIEW_EXECUTION` 任务。
4. 立即返回 `PENDING`，不等待文件解析和模型审核完成。
5. 解析协调器观察解析结果；解析成功且未截断时才把任务改为 `QUEUED` 并写入 task outbox。
6. 解析失败、格式错误、内容被截断或项目不可写时，同时将审核记录和等待任务标记为失败。

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "recordId": 10001,
    "projectId": 1,
    "templateId": 10,
    "fileId": 20001,
    "parseRecordId": 30001,
    "taskId": 40001,
    "status": "PENDING",
    "currentStage": "PARSING",
    "progress": 5,
    "ruleTotal": 12,
    "ruleCompleted": 0,
    "chunkTotal": 0
  },
  "requestId": "REQ-xxx",
  "timestamp": "2026-07-23T18:00:00+08:00"
}
```

不支持格式、模板无规则、文件上传失败、规则快照失败、任务/outbox 写入失败时不得返回成功。

## 2. 查询审查记录

```text
GET /api/review/records
GET /api/review/records/{recordId}
```

列表查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| projectId | Long | 否 | 项目 ID |
| status | String | 否 | 审查状态 |
| templateId | Long | 否 | 模板 ID |
| pageNo | int | 否 | 默认 1 |
| pageSize | int | 否 | 默认 20 |

记录状态：

```text
PENDING
PROCESSING
COMPLETED
FAILED
CANCELED
ARCHIVED
```

阶段：

```text
PARSING
QUEUED
CHUNKING
REVIEWING
REDUCING_RULES
SUMMARIZING
FINISHED
FAILED
CANCELED
```

详情响应重点字段：

```json
{
  "recordId": 10001,
  "status": "PROCESSING",
  "currentStage": "REVIEWING",
  "progress": 65,
  "ruleTotal": 12,
  "ruleCompleted": 7,
  "chunkTotal": 24,
  "overallStatus": null,
  "summary": null,
  "errorMessage": null
}
```

## 3. 查询规则审核结果

```text
GET /api/review/records/{recordId}/rule-results
GET /api/review/records/{recordId}/rule-results/{ruleCode}
```

规则最终状态：

```text
COMPLIANT
NON_COMPLIANT
PARTIALLY_COMPLIANT
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "ruleCode": "var_emergency_plan",
      "ruleDescription": "必须包含应急组织、联系人、处置流程和应急物资",
      "ruleOrder": 4,
      "executionStatus": "SUCCESS",
      "complianceStatus": "PARTIALLY_COMPLIANT",
      "reason": "文档包含应急组织、联系人和处置流程，但未提供应急物资清单",
      "suggestion": "补充应急物资名称、数量、存放地点和责任人",
      "evidence": [
        {
          "chunkId": 1024,
          "chunkCode": "CHUNK_0012",
          "evidenceType": "PARTIAL_EVIDENCE",
          "location": {
            "headingPath": "应急预案",
            "pageStart": 15,
            "pageEnd": 15,
            "sheetName": null,
            "rowStart": null,
            "rowEnd": null
          },
          "quote": "项目成立应急处置小组"
        }
      ],
      "confidence": 0.91,
      "modelTraceId": "trace-xxx",
      "errorMessage": null
    }
  ],
  "requestId": "REQ-xxx",
  "timestamp": "2026-07-23T18:10:00+08:00"
}
```

约束：

- `NON_COMPLIANT`、`PARTIALLY_COMPLIANT` 的 `reason` 和 `suggestion` 必须非空。
- 规则描述是提交时的快照，不随模板后续修改变化。
- 证据包含块、页码、章节或 Excel Sheet/行号定位。
- 记录未完成时返回全部规则快照及各自 `executionStatus`，失败规则必须暴露错误。

## 4. 块级审核与规则汇总契约

块级模型只返回证据观察，不直接决定整篇文档最终状态。内部观察值：

```text
SUPPORTING_EVIDENCE
VIOLATING_EVIDENCE
PARTIAL_EVIDENCE
NO_EVIDENCE
NOT_APPLICABLE
```

块级结果示例：

```json
{
  "chunkCode": "CHUNK_0012",
  "ruleCode": "var_emergency_plan",
  "observation": "PARTIAL_EVIDENCE",
  "matchedRequirements": [
    "应急组织",
    "联系人",
    "处置流程"
  ],
  "missingRequirements": [
    "应急物资"
  ],
  "reason": "当前块未发现应急物资信息",
  "evidence": [
    {
      "page": 15,
      "quote": "项目成立应急处置小组"
    }
  ],
  "confidence": 0.91
}
```

某一块返回 `NO_EVIDENCE` 不代表整篇文档不符合。必须汇总该规则的全部块级结果后，才能生成 `COMPLIANT`、`NON_COMPLIANT` 或 `PARTIALLY_COMPLIANT`。

规则汇总会携带全部观察计数和 matched/missing requirements；为避免长文档证据导致汇总请求再次超出上下文，只按证据类型携带有上限的代表性证据样本，完整有效证据仍保存于规则结果。

总体状态由 Java 确定：

```text
存在 NON_COMPLIANT
  -> overallStatus = NON_COMPLIANT

否则存在 PARTIALLY_COMPLIANT
  -> overallStatus = PARTIALLY_COMPLIANT

否则
  -> overallStatus = COMPLIANT
```

最终模型只生成摘要、统计、重点风险和建议，不得修改规则状态或总体状态。

## 5. 重试、取消和阶段日志

```text
POST /api/review/records/{recordId}/retry
POST /api/review/records/{recordId}/cancel
GET  /api/review/records/{recordId}/stages
```

重试规则：

- 只有 `FAILED` 记录允许重试。
- 解析成功时复用 Markdown，不重新解析。
- 切块成功时复用文档块。
- 已成功的规则最终结果不重复生成。
- 不全量持久化规则与文档块的中间观察；一条规则执行中途失败时，重新审核该规则的全部文档块。
- 只重试失败规则或未完成阶段。
- 超过最大重试次数时明确失败。

取消规则：

- `PENDING`、`PROCESSING` 允许取消。
- 终态记录不允许取消。
- Worker 在每个文档块模型调用前续租任务租约并检查取消请求，长文档执行不会只依赖领取任务时的初始租约。
- 运行中任务设置 `cancel_requested=true`，Worker 在阶段边界协作停止。

## 6. 删除和归档

```text
DELETE /api/review/records/{recordId}
POST /api/review/records/{recordId}/archive
```

删除为逻辑删除；仅 `COMPLETED`、`FAILED`、`CANCELED`、`ARCHIVED` 终态记录允许删除，非终态记录必须先取消。归档会将记录置为归档状态。

## 7. 更新问题状态

```text
PUT /api/review/records/{recordId}/issues/{issueId}
Content-Type: application/json
```

请求体：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| status | String | 是 | 问题处理状态 |
| comment | String | 否 | 处理说明 |

示例：

```bash
curl --noproxy '*' -X PUT "http://127.0.0.1:8080/api/review/records/1/issues/ISSUE-1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"RESOLVED","comment":"已补充整改材料"}'
```

## 8. 执行规则

- 提交审查记录后必须校验生成 ID 并读回持久化记录，失败时不调用 Python Agent。
- 审查模板必须包含已解析的 `{{ var_xxx : 审核规则描述 }}`，并在提交时快照当前规则。
- 审查规则为空、变量名为空或描述为空时必须将本次审查标记为失败，不能在没有模板规则的情况下生成通用审查结果。
- 被审核文件必须通过 file 模块解析为完整 Markdown；解析为空、失败或 `inputTruncated=true` 时不得审核。超过单次模型窗口的文件由 file 模块分批解析并按原顺序合并，完整提取文本超过 `FILE_PARSE_MAX_DOCUMENT_CHARS` 时明确失败。
- 长 Markdown 必须按结构切分，并保留页码、章节、Sheet 和行号定位。
- 开始逐块审核前按最坏情况校验 `REVIEW_MAX_MODEL_CALLS`，超限时明确失败，不允许无限放大“规则数 × 文档块数”的模型调用。
- 当前块无证据不能直接判定整篇文档不符合。
- 每条规则全部块完成后才能生成三态最终结果。
- 持久化采用 `review_record`、`review_document_chunk`、`review_rule_result` 三表结构，不建立全量 `review_rule_chunk_result`。
- `review_document_chunk` 只保存定位元数据、内容 Hash 和 MinIO 对象引用；`review_rule_result.evidenceJson` 只保存命中证据及其 `chunkId`。
- `NO_EVIDENCE`、`NOT_APPLICABLE` 等无有效证据的块级观察不写入 MySQL。
- Java 确定总体状态，模型只生成最终摘要。
- Python Agent 返回失败、空结果或无效 JSON 时，审查记录必须标记为 `FAILED` 并记录错误。
- 如果失败状态无法落库，必须返回冲突，不能丢失可观测性。
- 调用 Python Agent 必须记录外部调用日志。
- 外部调用日志不得保存完整 Markdown、文件临时 URL、Token 或密钥。
