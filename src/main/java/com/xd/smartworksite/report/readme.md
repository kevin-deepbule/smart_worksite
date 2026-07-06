# 模板与报告生成模块实现记录

## 范围约定

第一版目标是在 `smart_worksite` 中完成模板元数据管理和密评报告生成集成闭环。

- 模板功能只实现审查模板、报告模板上传和元数据管理。
- 模板元数据包括模板分类、模板类型、适用场景、版本、启停用状态和说明。
- 不实现模板变量解析、变量清单展示、变量说明维护。
- 报告生成调用 CryptoAgentV3，并使用 CryptoAgentV3 的默认密评报告模板。
- 用户上传的报告模板第一版只作为业务模板资产保存，不参与 CryptoAgentV3 渲染。

## 阶段 1：数据库结构

### 功能说明

新增 Flyway 迁移脚本：

- `src/main/resources/db/migration/V2__template_report_schema.sql`

新增数据表：

- `template`：统一保存审查模板和报告模板元数据。
- `report_config`：保存报告生成配置。
- `report`：保存报告主记录、状态、进度和当前版本。
- `report_version`：保存报告生成版本、Word/PDF 文件引用和引擎响应摘要。

### 关键设计

- 所有业务表均包含 `project_id`，用于项目隔离。
- 所有业务表均包含 `deleted`，用于逻辑删除。
- 所有业务表均包含 `created_at`、`updated_at`、`created_by`、`updated_by`。
- `template.template_category` 区分 `REVIEW` 和 `REPORT`。
- `report.template_id` 只记录用户选择的报告模板；第一版生成时 CryptoAgentV3 仍使用默认模板。
- 未创建模板变量表，符合第一版不做变量解析的范围。

### 测试用例

1. 执行 Maven 构建，验证新增 Flyway SQL 作为资源加载且工程可编译。

```bash
mvn test
```

结果：

- `BUILD SUCCESS`
- `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`
- 覆盖用例：模板上传元数据保存、报告生成调用 CryptoAgentV3 请求契约、重新生成复用原报告配置。

### 对抗式审查

- 检查 `V2__template_report_schema.sql` 中四张表均包含 `project_id` 和 `deleted`。
- 检查未新增 `template_variable` 或变量解析相关表，避免超出当前实现范围。
- 检查报告版本表记录 `source_snapshot` 和 `engine_response`，便于追踪第一版实际使用 `CRYPTO_AGENT_V3_DEFAULT` 模板。

结论：阶段 1 通过。


## 阶段 2：模板上传与元数据管理

### 功能说明

新增统一模板模块 `com.xd.smartworksite.template`，用于管理审查模板和报告模板。

已实现能力：

- 上传审查模板和报告模板。
- 保存模板文件到对象存储，并写入 `file_object`。
- 保存模板元数据到 `template`。
- 支持模板分类：`REVIEW`、`REPORT`。
- 支持模板类型、适用场景、版本号、说明。
- 支持模板启用和停用。
- 支持模板列表、详情、元数据修改、逻辑删除。
- 提供兼容接口 `/api/report/templates` 和 `/api/review/templates`。

未实现能力：

- 不解析模板变量。
- 不返回模板变量清单。
- 不维护变量说明。
- 用户上传的报告模板暂不参与 CryptoAgentV3 渲染。

### 主要代码

- `com.xd.smartworksite.template.controller.TemplateController`
- `com.xd.smartworksite.template.controller.ReportTemplateController`
- `com.xd.smartworksite.template.controller.ReviewTemplateController`
- `com.xd.smartworksite.template.application.TemplateApplicationService`
- `com.xd.smartworksite.template.repository.TemplateRepository`
- `src/main/resources/mapper/template/TemplateMapper.xml`

### 接口

#### 1. 统一上传模板

```http
POST /api/templates
Content-Type: multipart/form-data
```

参数：

- `projectId`: 项目 ID，必填。
- `templateCategory`: 模板分类，`REVIEW` 或 `REPORT`，必填。
- `templateName`: 模板名称，必填。
- `templateType`: 模板类型，必填。
- `scenario`: 适用场景，可选。
- `versionNo`: 版本号，可选，默认 `v1`。
- `description`: 说明，可选。
- `file`: 模板文件，必填。

#### 2. 查询模板分页列表

```http
GET /api/templates?projectId=1&templateCategory=REPORT&pageNo=1&pageSize=20
```

返回 `PageResult<TemplateResponse>`。

#### 3. 查询模板详情

```http
GET /api/templates/{templateId}
```

#### 4. 修改模板元数据

```http
PUT /api/templates/{templateId}
Content-Type: application/json
```

请求示例：

```json
{
  "templateName": "密评报告模板v1",
  "templateType": "CRYPTO_EVALUATION_REPORT",
  "scenario": "密评报告生成",
  "versionNo": "v1",
  "description": "第一版测试模板"
}
```

#### 5. 启用/停用模板

```http
POST /api/templates/{templateId}/enable
POST /api/templates/{templateId}/disable
```

#### 6. 删除模板

```http
DELETE /api/templates/{templateId}
```

#### 7. 报告模板兼容接口

```http
POST /api/report/templates
GET /api/report/templates?projectId=1
```

说明：

- 上传时自动设置 `templateCategory=REPORT`。
- 如果未传 `templateName`，使用上传文件名。
- 如果未传 `templateType`，默认 `CRYPTO_EVALUATION_REPORT`。
- 列表接口返回数组，兼容当前前端 mock 形态。

#### 8. 审查模板兼容接口

```http
POST /api/review/templates
GET /api/review/templates?projectId=1
```

说明：

- 上传时自动设置 `templateCategory=REVIEW`。
- 列表接口返回数组，兼容当前前端 mock 形态。

### 测试用例

#### 编译测试

```bash
mvn test
```

结果：

- `BUILD SUCCESS`
- `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`
- 覆盖用例：模板上传元数据保存、报告生成调用 CryptoAgentV3 请求契约、重新生成复用原报告配置。

#### 运行时接口测试

尝试启动后端：

```bash
mvn spring-boot:run
```

结果：

- 失败，原因是当前本地 MySQL 环境中默认用户 `worksite/worksite` 无法登录。
- 使用 `DB_USERNAME=root DB_PASSWORD=root` 再次启动后，连接成功但目标库 `smart_worksite` 不存在。

结论：

- 阶段 2 已通过编译级验证。
- HTTP 接口测试受本地数据库环境阻塞，需先按 `deploy/docker-compose-env.yml` 初始化正确的 `smart_worksite` 数据库和账号，或提供可用的 `DB_URL/DB_USERNAME/DB_PASSWORD`。

### 对抗式审查

检查项：

- Controller 没有直接调用 Mapper、MinIO 或 SQL。
- Application Service 负责业务编排和事务边界。
- Mapper SQL 默认过滤 `deleted = 0`。
- 上传模板同时写 `file_object` 和 `template`，并回写 `file_object.biz_id`。
- `/api/report/templates` 与 `/api/review/templates` 不允许前端传错模板分类，分类由路径决定。
- 模板变量解析相关能力未引入，符合当前范围。

已知技术债：

- 当前 `file` 模块尚无文件元数据应用服务，因此模板模块临时通过 `TemplateMapper` 写 `file_object`。后续 file facade 完成后，应收敛为跨模块应用服务调用。
- 当前创建人/更新人暂用 `1`，后续接入认证上下文后应改为真实用户 ID。

结论：阶段 2 编译验证通过，运行时接口测试等待数据库环境修复后执行。


## 阶段 3：报告配置、报告记录和生成任务基础接口

### 功能说明

已实现报告生成基础接口，但本阶段不调用 CryptoAgentV3。

已实现能力：

- 创建报告生成配置 `report_config`。
- 创建报告主记录 `report`。
- 创建异步任务记录 `generate_task`。
- 查询报告分页列表。
- 查询报告详情。
- 重新生成报告时基于原报告信息创建新的报告任务。

### 接口

#### 1. 创建报告生成任务

```http
POST /api/reports
Content-Type: application/json
```

请求示例：

```json
{
  "projectId": 1,
  "reportName": "密评报告测试",
  "reportType": "CRYPTO_EVALUATION_REPORT",
  "templateId": 1001,
  "referenceFileIds": [5001],
  "knowledgeBaseIds": [],
  "dataSourceIds": [],
  "generationParams": {
    "engineType": "CRYPTO_AGENT_V3"
  }
}
```

响应示例：

```json
{
  "reportId": 2001,
  "taskId": 9501,
  "status": "PROCESSING"
}
```

本阶段实际写入的报告状态为 `PENDING`，任务状态为 `PENDING`。响应中的 `PROCESSING` 表示任务已提交给生成链路，后续阶段接入 CryptoAgentV3 后会更新为真实执行状态。

#### 2. 查询报告列表

```http
GET /api/reports?projectId=1&pageNo=1&pageSize=20&status=PENDING
```

#### 3. 查询报告详情

```http
GET /api/reports/{reportId}
```

#### 4. 重新生成报告

```http
POST /api/reports/{reportId}/regenerate
```

### 测试用例

#### 编译测试

```bash
mvn test
```

结果：

- `BUILD SUCCESS`
- `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`
- 覆盖用例：模板上传元数据保存、报告生成调用 CryptoAgentV3 请求契约、重新生成复用原报告配置。

#### 运行时接口测试

仍受本地数据库环境阻塞：

- 默认 `worksite/worksite` 无法登录当前 MySQL。
- 使用 `root/root` 时目标库 `smart_worksite` 不存在。

因此本阶段完成编译级验证和代码审查，HTTP 测试需待数据库环境修复后执行。

### 对抗式审查

检查项：

- `ReportController` 只做 HTTP 入参和统一响应，不直接调用 Mapper。
- `ReportGenerationApplicationService` 校验项目存在。
- 如果传入 `templateId`，必须满足：模板存在、同项目、`templateCategory=REPORT`、`status=ENABLED`。
- 本阶段没有任何 CryptoAgentV3、HTTP client 或外部引擎调用。
- `ReportMapper.xml` 查询均过滤 `deleted = 0`。
- `generate_task` 只保存任务记录，不伪造生成成功状态。

已知限制：

- `referenceFileIds`、`knowledgeBaseIds`、`dataSourceIds` 只保存为 JSON，不做跨模块存在性校验。后续接入对应模块 facade 后补齐。
- 重新生成报告已按原报告 `config_id` 读取 `report_config`，复用 `referenceFileIds`、`knowledgeBaseIds`、`dataSourceIds` 和 `generationParams`。

结论：阶段 3 单元测试和编译验证通过，运行时接口测试等待数据库环境修复后执行。


## 阶段 4：CryptoAgentV3 集成、报告版本和下载

### 功能说明

已实现 Java 后端调用 CryptoAgentV3 生成密评报告，并保存 Word 结果。

已实现能力：

- `POST /api/reports` 创建报告后同步调用 CryptoAgentV3。
- 调用 CryptoAgentV3 时不传 `templateFile`，由 CryptoAgentV3 使用默认密评模板。
- `templateVariables` 固定传空对象 `{}`。
- 支持两种参考文档来源：
  - `generationParams.referenceDocuments` 中直接传已解析文本。
  - `referenceFileIds` 指向文本类文件，后端从 MinIO 读取 UTF-8 文本。
- CryptoAgentV3 返回 DOCX 后，Java 通过 `downloadRef` 下载生成文件。
- 生成文件上传到 MinIO，并写入 `file_object`，`biz_type=REPORT_OUTPUT`。
- 写入 `report_version`，记录 `source_snapshot` 和 `engine_response`。
- 报告成功后更新 `report.current_version_id/status/progress`。
- 报告失败后更新 `report.status=FAILED`、`generate_task.status=FAILED` 和错误原因。
- 新增下载接口，返回 Word 文件的 MinIO 预签名 URL。

### CryptoAgentV3 调用约定

Java 调用地址通过配置控制：

```yaml
app:
  report:
    crypto-agent-v3:
      base-url: ${CRYPTO_AGENT_V3_BASE_URL:http://127.0.0.1:8012}
      invoke-path: ${CRYPTO_AGENT_V3_INVOKE_PATH:/v1/report-generation/invoke}
      connect-timeout-seconds: ${CRYPTO_AGENT_V3_CONNECT_TIMEOUT_SECONDS:5}
      read-timeout-seconds: ${CRYPTO_AGENT_V3_READ_TIMEOUT_SECONDS:300}
```

请求示例：

```json
{
  "taskId": "9501",
  "reportId": "2001",
  "templateVariables": {},
  "referenceDocuments": [
    {
      "fileId": "5001",
      "fileName": "密测过程信息.txt",
      "content": "已解析的密测过程文本"
    }
  ]
}
```

注意：

- 不传 `templateFile`。
- CryptoAgentV3 使用默认密评模板。
- `referenceDocuments` 不能为空，这是 CryptoAgentV3 当前 schema 要求。

### 报告创建接口增强

```http
POST /api/reports
Content-Type: application/json
```

推荐第一阶段测试请求：

```json
{
  "projectId": 1,
  "reportName": "密评报告测试",
  "reportType": "CRYPTO_EVALUATION_REPORT",
  "templateId": 1001,
  "generationParams": {
    "referenceDocuments": [
      {
        "fileId": "manual-1",
        "fileName": "密测过程信息.txt",
        "content": "这里放密测过程信息的已解析文本。"
      }
    ]
  }
}
```

如果不传 `generationParams.referenceDocuments`，则必须传 `referenceFileIds`，且这些文件当前必须是文本类文件：

- `text/*`
- `.txt`
- `.md`
- `.json`
- `.csv`

Word/PDF 参考文档解析不在本模块第一版实现范围内，应由文件解析模块提供已解析文本后再传入。

### 下载接口

```http
GET /api/reports/{reportId}/download?format=WORD
```

返回：

```json
{
  "code": 0,
  "message": "success",
  "data": "http://minio-presigned-url"
}
```

说明：

- 当前仅支持 `WORD`。
- 请求 `PDF` 会返回业务错误：`当前版本尚未生成PDF报告`。

### 测试用例

#### 编译测试

```bash
mvn test
```

结果：

- `BUILD SUCCESS`
- `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`
- 覆盖用例：模板上传元数据保存、报告生成调用 CryptoAgentV3 请求契约、重新生成复用原报告配置。

#### 运行时接口测试

仍受本地数据库环境阻塞：

- 默认 `worksite/worksite` 无法登录当前 MySQL。
- 使用 `root/root` 时目标库 `smart_worksite` 不存在。

CryptoAgentV3 集成测试还依赖：

- CryptoAgentV3 后端服务可访问。
- 默认配置为 `http://127.0.0.1:8012/v1/report-generation/invoke`。
- 如实际服务运行在其他端口，需要设置环境变量 `CRYPTO_AGENT_V3_BASE_URL`，或修改 `app.report.crypto-agent-v3.base-url`。
- CryptoAgentV3 返回的 `downloadRef` 必须能被 Java 后端访问。

### 对抗式审查

检查项：

- 未把用户上传模板路径传给 CryptoAgentV3，符合“默认模板生成”的阶段范围。
- `referenceDocuments` 不能为空；没有伪造空文档或 mock 内容。
- 非文本类 `referenceFileIds` 会明确失败，提示通过 `generationParams.referenceDocuments` 提供已解析文本。
- CryptoAgentV3 返回失败、无 DOCX、无 `downloadRef`、下载空文件都会标记报告失败。
- PDF 未生成时明确返回业务错误，不用 Word 冒充 PDF。
- `source_snapshot.templateUsedByEngine` 固定记录 `CRYPTO_AGENT_V3_DEFAULT`，便于审计。
- `templateId` 可以为空，已修复 `source_snapshot` 对空模板 ID 的处理。

已知限制：

- 当前 `POST /api/reports` 是同步调用 CryptoAgentV3，适合一周内演示；后续应改为 Redis 队列 + 后台 Worker。
- 尚未写 `task_stage_log` 阶段日志，只更新 `generate_task.current_stage/status/error_message`。后续任务模块稳定后应补阶段日志写入。
- `report_version.version_no` 当前固定为 `1`。后续重新生成应基于历史版本递增。
- 下载接口返回预签名 URL，不是直接文件流；前端下载按钮需要按 URL 跳转或二次下载。

结论：阶段 4 单元测试和编译验证通过，运行时端到端测试等待数据库和 CryptoAgentV3 服务环境修复后执行。


## 阶段 5：单元测试补强与对抗式审查

### 功能说明

为避免只停留在编译验证，新增不依赖 MySQL、MinIO、CryptoAgentV3 实例的应用服务单元测试。

新增测试文件：

- `src/test/java/com/xd/smartworksite/template/application/TemplateApplicationServiceTest.java`
- `src/test/java/com/xd/smartworksite/report/application/ReportGenerationApplicationServiceTest.java`

覆盖内容：

- 报告模板上传会保存 `file_object`，并保存 `template` 元数据。
- 报告模板默认启用，版本号缺省为 `v1`。
- 报告生成调用 CryptoAgentV3 时只传 `templateVariables={}` 和 `referenceDocuments`，不传用户上传模板文件。
- CryptoAgentV3 返回 DOCX 后，后端保存 `REPORT_OUTPUT` 文件和 `report_version`。
- 重新生成报告时从原报告 `report_config` 复用参考文档和生成参数。

### 测试用例

```bash
mvn test
```

结果：

- `BUILD SUCCESS`
- `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

### 对抗式审查

检查项：

- 单元测试不依赖真实数据库、对象存储或 CryptoAgentV3 服务，验证的是应用服务业务契约。
- 报告生成测试使用本地 HTTP 服务提供 DOCX 下载，覆盖 Java 后端下载并保存生成文件的真实分支。
- 重新生成报告不再丢失 `generationParams.referenceDocuments`，避免演示时原报告可生成但重新生成失败。
- 测试没有 mock 空参考文档，也没有把上传模板作为 CryptoAgentV3 输入，符合当前范围。

结论：阶段 5 通过。

## 阶段 6：报告生成与下载解耦

### 功能说明

根据联调结果，CryptoAgentV3 已能成功生成报告并返回 `downloadRef`，但其下载接口可能受自身输出目录白名单限制。为避免报告生成接口被下载动作阻塞，调整为两阶段流程：

- `POST /api/reports` 只负责调用 CryptoAgentV3 生成报告。
- 生成成功后保存 CryptoAgentV3 返回的 `downloadRef` 到 `report.preview_url`。
- 生成成功后写入 `report_version`，记录完整 `engine_response`，但 `word_file_id` 暂为空。
- 不在生成接口内下载 DOCX，不在生成接口内上传 MinIO。
- `GET /api/reports/{reportId}/download?format=WORD` 负责按需下载：如果本地已有 `word_file_id`，直接返回 MinIO 预签名地址；如果没有，则从 `report.preview_url` 下载 CryptoAgentV3 文件，保存 MinIO，回写当前版本 `word_file_id/content_hash`，再返回 MinIO 预签名地址。

### 接口变化

#### 创建报告

```http
POST /api/reports
```

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "reportId": 8,
    "taskId": 8,
    "status": "SUCCESS"
  }
}
```

说明：

- `SUCCESS` 表示 CryptoAgentV3 已生成报告并返回下载链接。
- 可通过报告详情中的 `previewUrl` 查看 CryptoAgentV3 返回的原始下载链接。
- 此时可能尚未保存 smart_worksite 自己的 Word 文件对象。

#### 查询报告详情

```http
GET /api/reports/{reportId}
```

重点字段：

```json
{
  "status": "SUCCESS",
  "previewUrl": "http://127.0.0.1:8012/v1/report/download?path=..."
}
```

#### 下载 Word 报告

```http
GET /api/reports/{reportId}/download?format=WORD
```

行为：

- 已保存到 MinIO：直接返回 MinIO 预签名地址。
- 未保存到 MinIO：从 `previewUrl` 下载 CryptoAgentV3 DOCX，保存 MinIO 后返回 MinIO 预签名地址。

### 测试用例

```bash
mvn test
```

结果：

- `BUILD SUCCESS`
- `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`

新增覆盖：

- 创建报告时不下载 Word，只保存 CryptoAgentV3 下载链接。
- 下载接口按需保存 Word，并返回 smart_worksite 的访问地址。

### 对抗式审查

检查项：

- 生成接口不再依赖 CryptoAgentV3 下载接口白名单，因此 CryptoAgentV3 生成成功不会因为后续下载 403 被标记为失败。
- 下载接口仍然保持真实下载和文件保存，不伪造文件、不绕过对象存储。
- `report_version.engine_response` 继续保存 CryptoAgentV3 完整返回，便于审计。
- `report.preview_url` 保存 CryptoAgentV3 返回的 `downloadRef`，作为延迟下载来源。
- 后续如果 CryptoAgentV3 下载接口仍返回 403，只会影响下载接口，不会影响报告生成状态。

结论：阶段 6 通过。
