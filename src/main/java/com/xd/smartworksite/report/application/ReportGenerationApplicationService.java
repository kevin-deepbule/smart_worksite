package com.xd.smartworksite.report.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.file.infra.StorageAdapter;
import com.xd.smartworksite.file.infra.StorageObject;
import com.xd.smartworksite.project.repository.ProjectRepository;
import com.xd.smartworksite.report.domain.GenerateTask;
import com.xd.smartworksite.report.domain.Report;
import com.xd.smartworksite.report.domain.ReportConfig;
import com.xd.smartworksite.report.domain.ReportEngineType;
import com.xd.smartworksite.report.domain.ReportStatus;
import com.xd.smartworksite.report.domain.ReportVersion;
import com.xd.smartworksite.report.dto.ReportCreateRequest;
import com.xd.smartworksite.report.dto.ReportCreateResponse;
import com.xd.smartworksite.report.dto.ReportQueryRequest;
import com.xd.smartworksite.report.dto.ReportResponse;
import com.xd.smartworksite.report.infra.CryptoAgentGenerateRequest;
import com.xd.smartworksite.report.infra.CryptoAgentGenerateResponse;
import com.xd.smartworksite.report.infra.CryptoAgentV3ReportClient;
import com.xd.smartworksite.report.infra.GeneratedFilePayload;
import com.xd.smartworksite.report.infra.ReferenceDocumentPayload;
import com.xd.smartworksite.report.repository.ReportRepository;
import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.domain.TemplateStatus;
import com.xd.smartworksite.template.repository.TemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportGenerationApplicationService {

    private static final String TASK_TYPE_REPORT_GENERATION = "REPORT_GENERATION";
    private static final String BIZ_TYPE_REPORT = "REPORT";
    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_PROCESSING = "PROCESSING";
    private static final String TASK_STATUS_SUCCESS = "SUCCESS";
    private static final String TASK_STATUS_FAILED = "FAILED";
    private static final String TASK_STAGE_CONFIG_VALIDATE = "CONFIG_VALIDATE";
    private static final String TASK_STAGE_CRYPTO_AGENT_GENERATION = "CRYPTO_AGENT_GENERATION";
    private static final String TASK_STAGE_REPORT_FILE_SAVE = "REPORT_FILE_SAVE";
    private static final String FILE_STATUS_ACTIVE = "ACTIVE";

    private final ReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final TemplateRepository templateRepository;
    private final StorageAdapter storageAdapter;
    private final CryptoAgentV3ReportClient cryptoAgentV3ReportClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ReportGenerationApplicationService(ReportRepository reportRepository,
                                              ProjectRepository projectRepository,
                                              TemplateRepository templateRepository,
                                              StorageAdapter storageAdapter,
                                              CryptoAgentV3ReportClient cryptoAgentV3ReportClient,
                                              ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.projectRepository = projectRepository;
        this.templateRepository = templateRepository;
        this.storageAdapter = storageAdapter;
        this.cryptoAgentV3ReportClient = cryptoAgentV3ReportClient;
        this.objectMapper = objectMapper;
    }

    public ReportCreateResponse createReport(ReportCreateRequest request) {
        requireProject(request.getProjectId());
        String reportType = normalizeRequired(request.getReportType(), "报告类型不能为空");
        Long templateId = request.getTemplateId();
        if (templateId != null) {
            validateReportTemplate(templateId, request.getProjectId());
        }

        String reportName = request.getReportName();
        if (reportName == null || reportName.isBlank()) {
            reportName = reportType + "报告";
        }

        ReportConfig config = saveConfig(request, reportType, reportName.trim(), templateId);
        Report report = saveReport(request, reportType, reportName.trim(), templateId, config.getId());
        GenerateTask task = saveTask(request.getProjectId(), report.getId());
        reportRepository.updateReportTask(report.getId(), task.getId());
        reportRepository.updateTaskBizId(task.getId(), report.getId());

        try {
            executeCryptoAgentGeneration(report, config, task);
            return new ReportCreateResponse(report.getId(), task.getId(), ReportStatus.SUCCESS.name());
        } catch (BusinessException ex) {
            markFailed(report.getId(), task.getId(), ex.getMessage());
            return new ReportCreateResponse(report.getId(), task.getId(), ReportStatus.FAILED.name());
        } catch (RuntimeException ex) {
            markFailed(report.getId(), task.getId(), ex.getMessage());
            return new ReportCreateResponse(report.getId(), task.getId(), ReportStatus.FAILED.name());
        }
    }

    public PageResult<ReportResponse> queryReports(ReportQueryRequest request) {
        Page<Report> page = PageHelper.startPage(request.getPageNo(), request.getPageSize())
                .doSelectPage(() -> reportRepository.findReportPage(
                        request.getProjectId(),
                        trimToNull(request.getReportType()),
                        normalizeOptional(request.getStatus()),
                        trimToNull(request.getKeyword())
                ));
        List<ReportResponse> records = page.getResult().stream().map(this::toResponse).toList();
        return new PageResult<>(request.getPageNo(), request.getPageSize(), page.getTotal(), records);
    }

    public ReportResponse getReport(Long reportId) {
        Report report = reportRepository.findReportById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报告不存在"));
        return toResponse(report);
    }

    public ReportCreateResponse regenerateReport(Long reportId) {
        Report report = reportRepository.findReportById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报告不存在"));
        ReportConfig config = reportRepository.findConfigById(report.getConfigId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报告生成配置不存在"));
        ReportCreateRequest request = new ReportCreateRequest();
        request.setProjectId(report.getProjectId());
        request.setReportName(report.getReportName());
        request.setReportType(report.getReportType());
        request.setTemplateId(report.getTemplateId());
        request.setReferenceFileIds(parseLongList(config.getReferenceFileIds()));
        request.setKnowledgeBaseIds(parseLongList(config.getKnowledgeBaseIds()));
        request.setDataSourceIds(parseLongList(config.getDataSourceIds()));
        request.setGenerationParams(parseObjectMap(config.getGenerationParams()));
        return createReport(request);
    }

    public String createDownloadUrl(Long reportId, String format) {
        String normalizedFormat = format == null || format.isBlank() ? "WORD" : format.trim().toUpperCase(Locale.ROOT);
        if (!"WORD".equals(normalizedFormat)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前版本尚未生成PDF报告");
        }
        return reportRepository.findCurrentWordFileId(reportId)
                .flatMap(reportRepository::findFileObjectById)
                .map(file -> storageAdapter.createAccessUrl(file.getObjectName(), Duration.ofMinutes(10)))
                .orElseGet(() -> saveGeneratedWordFromPreviewUrl(reportId));
    }

    private ReportConfig saveConfig(ReportCreateRequest request, String reportType, String reportName, Long templateId) {
        ReportConfig config = new ReportConfig();
        config.setProjectId(request.getProjectId());
        config.setConfigName(reportName);
        config.setReportType(reportType);
        config.setTemplateId(templateId);
        config.setReferenceFileIds(toJsonArray(request.getReferenceFileIds()));
        config.setKnowledgeBaseIds(toJsonArray(request.getKnowledgeBaseIds()));
        config.setDataSourceIds(toJsonArray(request.getDataSourceIds()));
        config.setGenerationParams(toJsonObject(request.getGenerationParams()));
        config.setStatus("SUBMITTED");
        return reportRepository.saveConfig(config);
    }

    private Report saveReport(ReportCreateRequest request, String reportType, String reportName, Long templateId, Long configId) {
        Report report = new Report();
        report.setProjectId(request.getProjectId());
        report.setConfigId(configId);
        report.setReportName(reportName);
        report.setReportType(reportType);
        report.setTemplateId(templateId);
        report.setEngineType(ReportEngineType.CRYPTO_AGENT_V3.name());
        report.setStatus(ReportStatus.PENDING.name());
        report.setProgress(0);
        return reportRepository.saveReport(report);
    }

    private GenerateTask saveTask(Long projectId, Long reportId) {
        GenerateTask task = new GenerateTask();
        task.setProjectId(projectId);
        task.setTaskType(TASK_TYPE_REPORT_GENERATION);
        task.setBizType(BIZ_TYPE_REPORT);
        task.setBizId(reportId);
        task.setStatus(TASK_STATUS_PENDING);
        task.setCurrentStage(TASK_STAGE_CONFIG_VALIDATE);
        task.setMaxRetryCount(3);
        return reportRepository.saveTask(task);
    }

    private void executeCryptoAgentGeneration(Report report, ReportConfig config, GenerateTask task) {
        reportRepository.updateReportProcessing(report.getId(), ReportStatus.PROCESSING.name(), 20, TASK_STAGE_CRYPTO_AGENT_GENERATION);
        reportRepository.updateTaskStatus(task.getId(), TASK_STATUS_PROCESSING, TASK_STAGE_CRYPTO_AGENT_GENERATION, null);

        List<ReferenceDocumentPayload> referenceDocuments = buildReferenceDocuments(config);
        validateReferenceDocuments(referenceDocuments);
        CryptoAgentGenerateRequest request = new CryptoAgentGenerateRequest();
        request.setTaskId(String.valueOf(task.getId()));
        request.setReportId(String.valueOf(report.getId()));
        request.setTemplateVariables(Map.of());
        request.setReferenceDocuments(referenceDocuments);

        CryptoAgentGenerateResponse response = cryptoAgentV3ReportClient.generate(request);
        if (!response.isSuccess()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    response.getErrorMessage() == null ? "CryptoAgentV3报告生成失败" : response.getErrorMessage());
        }

        GeneratedFilePayload docx = selectDocx(response);
        ReportVersion version = saveVersion(report, config, response, null, null);
        reportRepository.updateReportSuccess(report.getId(), version.getId(), ReportStatus.SUCCESS.name(), 100, docx.getDownloadRef());
        reportRepository.updateTaskStatus(task.getId(), TASK_STATUS_SUCCESS, "FINISH", null);
    }

    private List<ReferenceDocumentPayload> buildReferenceDocuments(ReportConfig config) {
        List<ReferenceDocumentPayload> explicitDocuments = explicitReferenceDocuments(config.getGenerationParams());
        if (!explicitDocuments.isEmpty()) {
            return explicitDocuments;
        }
        List<Long> fileIds = parseLongList(config.getReferenceFileIds());
        List<ReferenceDocumentPayload> documents = fileIds.stream().map(this::loadTextReferenceDocument).toList();
        if (documents.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "CryptoAgentV3要求至少提供一个参考文档内容");
        }
        return documents;
    }

    private void validateReferenceDocuments(List<ReferenceDocumentPayload> referenceDocuments) {
        if (referenceDocuments == null || referenceDocuments.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "CryptoAgentV3要求至少提供一个参考文档内容");
        }
        for (ReferenceDocumentPayload document : referenceDocuments) {
            if (document.getFileName() == null || document.getFileName().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "参考文档fileName不能为空");
            }
            if (document.getContent() == null || document.getContent().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "参考文档content不能为空: " + document.getFileName());
            }
        }
    }

    private List<ReferenceDocumentPayload> explicitReferenceDocuments(String generationParamsJson) {
        if (generationParamsJson == null || generationParamsJson.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> params = objectMapper.readValue(generationParamsJson, new TypeReference<>() {});
            Object documents = params.get("referenceDocuments");
            if (documents == null) {
                return List.of();
            }
            return objectMapper.convertValue(documents, new TypeReference<List<ReferenceDocumentPayload>>() {});
        } catch (IllegalArgumentException | JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "referenceDocuments参数格式错误");
        }
    }

    private ReferenceDocumentPayload loadTextReferenceDocument(Long fileId) {
        FileObjectRecord file = reportRepository.findFileObjectById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "参考文件不存在: " + fileId));
        if (!isTextFile(file)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "当前阶段只能从文本类参考文件读取内容，请在generationParams.referenceDocuments中提供已解析文本: " + file.getFileName());
        }
        try (InputStream inputStream = storageAdapter.download(file.getObjectName())) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "参考文件内容为空: " + file.getFileName());
            }
            return new ReferenceDocumentPayload(String.valueOf(file.getId()), file.getFileName(), content);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取参考文件失败: " + file.getFileName());
        }
    }

    private boolean isTextFile(FileObjectRecord file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("text/")) {
            return true;
        }
        String name = file.getFileName() == null ? "" : file.getFileName().toLowerCase(Locale.ROOT);
        return name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".json") || name.endsWith(".csv");
    }

    private GeneratedFilePayload selectDocx(CryptoAgentGenerateResponse response) {
        if (response.getGeneratedFiles() == null || response.getGeneratedFiles().isEmpty()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "CryptoAgentV3未返回生成文件");
        }
        return response.getGeneratedFiles().stream()
                .filter(file -> "DOCX".equalsIgnoreCase(file.getFileType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "CryptoAgentV3未返回DOCX文件"));
    }

    private byte[] downloadGeneratedFile(String downloadUrl) {
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报告下载地址不存在");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "下载CryptoAgentV3生成文件失败: HTTP " + response.statusCode());
            }
            if (response.body().length == 0) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "CryptoAgentV3生成文件为空");
            }
            return response.body();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "下载CryptoAgentV3生成文件失败: " + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "下载CryptoAgentV3生成文件被中断");
        }
    }

    private String saveGeneratedWordFromPreviewUrl(Long reportId) {
        Report report = reportRepository.findReportById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报告不存在"));
        if (!ReportStatus.SUCCESS.name().equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报告尚未生成成功");
        }
        byte[] reportBytes = downloadGeneratedFile(report.getPreviewUrl());
        GeneratedFilePayload generatedFile = new GeneratedFilePayload();
        generatedFile.setFileType("DOCX");
        generatedFile.setFileName(report.getReportName() + ".docx");
        generatedFile.setDownloadRef(report.getPreviewUrl());
        Long wordFileId = saveGeneratedWord(report, generatedFile, reportBytes);
        if (report.getCurrentVersionId() == null) {
            ReportVersion version = saveVersion(report, null, null, wordFileId, reportBytes);
            reportRepository.updateReportSuccess(report.getId(), version.getId(), ReportStatus.SUCCESS.name(), 100, report.getPreviewUrl());
        } else {
            reportRepository.updateVersionWordFile(report.getCurrentVersionId(), wordFileId, sha256(reportBytes));
        }
        FileObjectRecord file = reportRepository.findFileObjectById(wordFileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报告文件记录不存在"));
        return storageAdapter.createAccessUrl(file.getObjectName(), Duration.ofMinutes(10));
    }

    private Long saveGeneratedWord(Report report, GeneratedFilePayload generatedFile, byte[] bytes) {
        String filename = generatedFile.getFileName() == null || generatedFile.getFileName().isBlank()
                ? "report-" + report.getId() + ".docx"
                : generatedFile.getFileName();
        String objectName = "reports/project-" + report.getProjectId() + "/report-" + report.getId() + "/"
                + LocalDate.now() + "/" + UUID.randomUUID() + ".docx";
        StorageObject object = storageAdapter.upload(objectName, new ByteArrayInputStream(bytes), bytes.length,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        FileObjectRecord fileObject = new FileObjectRecord();
        fileObject.setProjectId(report.getProjectId());
        fileObject.setBizType("REPORT_OUTPUT");
        fileObject.setBizId(report.getId());
        fileObject.setFileName(filename);
        fileObject.setObjectName(object.getObjectName());
        fileObject.setContentType(object.getContentType());
        fileObject.setFileSize(object.getSize());
        fileObject.setStatus("ACTIVE");
        fileObject.setMetadata("{}");
        reportRepository.saveFileObject(fileObject);
        return fileObject.getId();
    }

    private ReportVersion saveVersion(Report report,
                                      ReportConfig config,
                                      CryptoAgentGenerateResponse response,
                                      Long wordFileId,
                                      byte[] reportBytes) {
        ReportVersion version = new ReportVersion();
        version.setProjectId(report.getProjectId());
        version.setReportId(report.getId());
        version.setVersionNo(1);
        version.setWordFileId(wordFileId);
        Map<String, Object> sourceSnapshot = new LinkedHashMap<>();
        sourceSnapshot.put("templateId", config == null ? report.getTemplateId() : config.getTemplateId());
        sourceSnapshot.put("templateUsedByEngine", "CRYPTO_AGENT_V3_DEFAULT");
        sourceSnapshot.put("referenceFileIds", config == null ? List.of() : parseLongList(config.getReferenceFileIds()));
        sourceSnapshot.put("engineType", ReportEngineType.CRYPTO_AGENT_V3.name());
        version.setSourceSnapshot(toJson(sourceSnapshot));
        version.setEngineResponse(response == null ? "{}" : toJson(response));
        version.setContentHash(reportBytes == null ? null : sha256(reportBytes));
        version.setStatus("SUCCESS");
        reportRepository.saveVersion(version);
        return version;
    }

    private void markFailed(Long reportId, Long taskId, String message) {
        String errorMessage = message == null || message.isBlank() ? "报告生成失败" : message;
        reportRepository.updateReportFailed(reportId, ReportStatus.FAILED.name(), errorMessage);
        reportRepository.updateTaskStatus(taskId, TASK_STATUS_FAILED, TASK_STAGE_CRYPTO_AGENT_GENERATION, errorMessage);
    }

    private void requireProject(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "项目ID不能为空");
        }
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "项目不存在"));
    }

    private void validateReportTemplate(Long templateId, Long projectId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报告模板不存在"));
        if (!projectId.equals(template.getProjectId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "报告模板不属于当前项目");
        }
        if (!TemplateCategory.REPORT.name().equals(template.getTemplateCategory())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择报告模板");
        }
        if (!TemplateStatus.ENABLED.name().equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报告模板未启用");
        }
    }

    private List<Long> parseLongList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "报告配置解析失败");
        }
    }

    private Map<String, Object> parseObjectMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "报告配置解析失败");
        }
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String toJsonArray(List<Long> value) {
        return toJson(value == null ? List.of() : value);
    }

    private String toJsonObject(Object value) {
        return toJson(value == null ? java.util.Map.of() : value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "JSON序列化失败");
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "SHA-256算法不可用");
        }
    }

    private ReportResponse toResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setReportId(report.getId());
        response.setProjectId(report.getProjectId());
        response.setTaskId(report.getTaskId());
        response.setReportName(report.getReportName());
        response.setReportType(report.getReportType());
        response.setTemplateId(report.getTemplateId());
        response.setEngineType(report.getEngineType());
        response.setVersion(report.getCurrentVersionId() == null ? "v0" : "v" + report.getCurrentVersionId());
        response.setStatus(report.getStatus());
        response.setProgress(report.getProgress());
        response.setPreviewUrl(report.getPreviewUrl());
        response.setErrorMessage(report.getErrorMessage());
        response.setCreatedBy("admin");
        response.setCreatedAt(report.getCreatedAt());
        response.setUpdatedAt(report.getUpdatedAt());
        return response;
    }
}
