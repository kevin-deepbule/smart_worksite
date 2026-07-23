package com.xd.smartworksite.review.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xd.smartworksite.ai.dto.AgentInvokeRequest;
import com.xd.smartworksite.ai.dto.AgentInvokeResponse;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.common.security.SecurityUtils;
import com.xd.smartworksite.file.application.FileDerivedContentApplicationService;
import com.xd.smartworksite.file.application.FileObjectApplicationService;
import com.xd.smartworksite.file.application.FileParseApplicationService;
import com.xd.smartworksite.file.domain.FileParseStatus;
import com.xd.smartworksite.file.dto.FileObjectResponse;
import com.xd.smartworksite.file.dto.FileParseContentResponse;
import com.xd.smartworksite.file.dto.FileParseRecordResponse;
import com.xd.smartworksite.file.dto.FileParseRequest;
import com.xd.smartworksite.file.dto.FileUploadRequest;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.review.domain.ReviewComplianceStatus;
import com.xd.smartworksite.review.domain.ReviewDocumentChunk;
import com.xd.smartworksite.review.domain.ReviewRecord;
import com.xd.smartworksite.review.domain.ReviewRuleExecutionStatus;
import com.xd.smartworksite.review.domain.ReviewRuleResult;
import com.xd.smartworksite.review.domain.ReviewStatus;
import com.xd.smartworksite.review.dto.ReviewIssueUpdateRequest;
import com.xd.smartworksite.review.dto.ReviewRecordQueryRequest;
import com.xd.smartworksite.review.dto.ReviewRecordResponse;
import com.xd.smartworksite.review.dto.ReviewRuleResultResponse;
import com.xd.smartworksite.review.dto.ReviewSubmitRequest;
import com.xd.smartworksite.review.infra.MarkdownReviewChunk;
import com.xd.smartworksite.review.infra.MarkdownReviewChunker;
import com.xd.smartworksite.review.repository.ReviewRecordRepository;
import com.xd.smartworksite.task.application.TaskApplicationService;
import com.xd.smartworksite.task.application.TaskSubmissionApplicationService;
import com.xd.smartworksite.task.application.TaskWorkerApplicationService;
import com.xd.smartworksite.task.dto.TaskResponse;
import com.xd.smartworksite.task.dto.TaskStageLogResponse;
import com.xd.smartworksite.template.application.TemplateApplicationService;
import com.xd.smartworksite.template.application.TemplateVariableApplicationService;
import com.xd.smartworksite.template.dto.TemplateResponse;
import com.xd.smartworksite.template.dto.TemplateVariableDescriptionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ReviewApplicationService {
    private static final int MAX_ERROR_LENGTH = 2000;
    private static final int MAX_QUOTE_LENGTH = 1000;
    private static final int MAX_REDUCTION_EVIDENCE_SAMPLES_PER_TYPE = 20;
    private static final Set<String> REVIEW_EXTENSIONS = Set.of("doc", "docx", "pdf", "txt", "xls", "xlsx");
    private static final Set<String> EVIDENCE_OBSERVATIONS = Set.of(
            "SUPPORTING_EVIDENCE", "VIOLATING_EVIDENCE", "PARTIAL_EVIDENCE");
    private static final Set<String> ALL_OBSERVATIONS = Set.of(
            "SUPPORTING_EVIDENCE", "VIOLATING_EVIDENCE", "PARTIAL_EVIDENCE",
            "NO_EVIDENCE", "NOT_APPLICABLE");

    private final ReviewRecordRepository reviewRecordRepository;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final FileObjectApplicationService fileObjectApplicationService;
    private final FileParseApplicationService fileParseApplicationService;
    private final FileDerivedContentApplicationService fileDerivedContentApplicationService;
    private final TemplateApplicationService templateApplicationService;
    private final TemplateVariableApplicationService templateVariableApplicationService;
    private final ReviewSubmissionPersistenceService reviewSubmissionPersistenceService;
    private final ReviewRetryPersistenceService reviewRetryPersistenceService;
    private final ReviewAiGateway reviewAiGateway;
    private final MarkdownReviewChunker markdownReviewChunker;
    private final ReviewProperties reviewProperties;
    private final TaskApplicationService taskApplicationService;
    private final TaskSubmissionApplicationService taskSubmissionApplicationService;
    private final TaskWorkerApplicationService taskWorkerApplicationService;
    private final ObjectMapper objectMapper;

    public ReviewApplicationService(ReviewRecordRepository reviewRecordRepository,
                                    ProjectAccessApplicationService projectAccessApplicationService,
                                    FileObjectApplicationService fileObjectApplicationService,
                                    FileParseApplicationService fileParseApplicationService,
                                    FileDerivedContentApplicationService fileDerivedContentApplicationService,
                                    TemplateApplicationService templateApplicationService,
                                    TemplateVariableApplicationService templateVariableApplicationService,
                                    ReviewSubmissionPersistenceService reviewSubmissionPersistenceService,
                                    ReviewRetryPersistenceService reviewRetryPersistenceService,
                                    ReviewAiGateway reviewAiGateway,
                                    MarkdownReviewChunker markdownReviewChunker,
                                    ReviewProperties reviewProperties,
                                    TaskApplicationService taskApplicationService,
                                    TaskSubmissionApplicationService taskSubmissionApplicationService,
                                    TaskWorkerApplicationService taskWorkerApplicationService,
                                    ObjectMapper objectMapper) {
        this.reviewRecordRepository = reviewRecordRepository;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.fileObjectApplicationService = fileObjectApplicationService;
        this.fileParseApplicationService = fileParseApplicationService;
        this.fileDerivedContentApplicationService = fileDerivedContentApplicationService;
        this.templateApplicationService = templateApplicationService;
        this.templateVariableApplicationService = templateVariableApplicationService;
        this.reviewSubmissionPersistenceService = reviewSubmissionPersistenceService;
        this.reviewRetryPersistenceService = reviewRetryPersistenceService;
        this.reviewAiGateway = reviewAiGateway;
        this.markdownReviewChunker = markdownReviewChunker;
        this.reviewProperties = reviewProperties;
        this.taskApplicationService = taskApplicationService;
        this.taskSubmissionApplicationService = taskSubmissionApplicationService;
        this.taskWorkerApplicationService = taskWorkerApplicationService;
        this.objectMapper = objectMapper;
    }

    public ReviewRecordResponse submitReview(ReviewSubmitRequest request) {
        projectAccessApplicationService.requireProjectWritableAccess(request.getProjectId());
        TemplateResponse template = requireReviewTemplate(request.getProjectId(), request.getTemplateId());
        List<TemplateVariableDescriptionResponse> rules = requireReviewRules(template.getTemplateId());
        validateReviewFile(request);

        FileUploadRequest uploadRequest = new FileUploadRequest();
        uploadRequest.setProjectId(request.getProjectId());
        uploadRequest.setBizType("REVIEW_DOC");
        uploadRequest.setFile(request.getFile());
        FileObjectResponse file = fileObjectApplicationService.upload(uploadRequest);

        try {
            FileParseRequest parseRequest = new FileParseRequest();
            parseRequest.setProjectId(request.getProjectId());
            parseRequest.setTargetFormat("MARKDOWN");
            parseRequest.setForce(false);
            FileParseRecordResponse parseRecord =
                    fileParseApplicationService.createParse(file.getFileId(), parseRequest);

            ReviewRecord record = reviewSubmissionPersistenceService.create(
                    request.getProjectId(),
                    template.getTemplateId(),
                    template.getFileId(),
                    file.getFileId(),
                    parseRecord.getRecordId(),
                    rules,
                    SecurityUtils.getCurrentUserId());
            return toResponse(record);
        } catch (RuntimeException ex) {
            try {
                fileObjectApplicationService.deleteFile(file.getFileId());
            } catch (RuntimeException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            throw ex;
        }
    }

    public PageResult<ReviewRecordResponse> queryRecords(ReviewRecordQueryRequest request) {
        if (request.getProjectId() != null) {
            projectAccessApplicationService.requireProjectAccess(request.getProjectId());
        }
        List<Long> accessibleProjectIds = request.getProjectId() == null && !SecurityUtils.isPlatformAdmin()
                ? projectAccessApplicationService.currentUserAccessibleProjectIds()
                : null;
        if (request.getProjectId() == null && accessibleProjectIds != null && accessibleProjectIds.isEmpty()) {
            return new PageResult<>(request.getPageNo(), request.getPageSize(), 0, List.of());
        }
        Page<ReviewRecord> page = PageHelper.startPage(request.getPageNo(), request.getPageSize())
                .doSelectPage(() -> reviewRecordRepository.findPage(
                        request.getProjectId(),
                        accessibleProjectIds,
                        request.getTemplateId(),
                        normalizeStatus(request.getStatus())
                ));
        return new PageResult<>(
                request.getPageNo(),
                request.getPageSize(),
                page.getTotal(),
                page.getResult().stream().map(this::toResponse).toList()
        );
    }

    public ReviewRecordResponse getRecord(Long recordId) {
        return toResponse(requireRecordAccess(recordId));
    }

    public List<ReviewRuleResultResponse> getRuleResults(Long recordId) {
        requireRecordAccess(recordId);
        return reviewRecordRepository.findRuleResults(recordId).stream().map(this::toRuleResponse).toList();
    }

    public ReviewRuleResultResponse getRuleResult(Long recordId, String ruleCode) {
        requireRecordAccess(recordId);
        String normalizedCode = normalizeRequired(ruleCode, "ruleCode is required");
        return reviewRecordRepository.findRuleResult(recordId, normalizedCode)
                .map(this::toRuleResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "review rule result not found"));
    }

    public List<TaskStageLogResponse> getStages(Long recordId) {
        ReviewRecord record = requireRecordAccess(recordId);
        return taskApplicationService.getStages(record.getTaskId());
    }

    public ReviewRecordResponse retry(Long recordId) {
        ReviewRecord record = requireRecordWritableAccess(recordId);
        if (!ReviewStatus.FAILED.name().equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "only failed review records can be retried");
        }
        FileParseRecordResponse parseRecord =
                fileParseApplicationService.getParseRecordForSystem(record.getParseRecordId());
        if (FileParseStatus.SUCCESS.name().equals(parseRecord.getStatus())) {
            taskApplicationService.retryTask(record.getTaskId());
            return getRecord(recordId);
        }

        FileParseRecordResponse nextParse = FileParseStatus.FAILED.name().equals(parseRecord.getStatus())
                ? fileParseApplicationService.retryParse(parseRecord.getRecordId())
                : parseRecord;
        reviewRetryPersistenceService.resetToParseWaiting(
                recordId, nextParse.getRecordId(), record.getTaskId(), SecurityUtils.getCurrentUserId());
        return getRecord(recordId);
    }

    @Transactional
    public ReviewRecordResponse cancel(Long recordId) {
        ReviewRecord record = requireRecordWritableAccess(recordId);
        if (record.getTaskId() == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "review task is missing");
        }
        TaskResponse task = taskApplicationService.cancelTask(record.getTaskId());
        if ("CANCELED".equals(task.getStatus())) {
            int updated = reviewRecordRepository.markCanceled(recordId, SecurityUtils.getCurrentUserId());
            if (updated == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "review cancel state changed");
            }
        }
        return getRecord(recordId);
    }

    @Transactional
    public void delete(Long recordId) {
        ReviewRecord record = requireRecordWritableAccess(recordId);
        if (!List.of(ReviewStatus.COMPLETED.name(), ReviewStatus.FAILED.name(),
                ReviewStatus.CANCELED.name(), ReviewStatus.ARCHIVED.name()).contains(record.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "non-terminal review record must be canceled before deletion");
        }
        Long operatorId = SecurityUtils.getCurrentUserId();
        reviewRecordRepository.softDeleteRules(recordId, operatorId);
        reviewRecordRepository.softDeleteChunks(recordId, operatorId);
        int updated = reviewRecordRepository.softDelete(recordId, operatorId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "review record delete failed");
        }
    }

    @Transactional
    public ReviewRecordResponse archive(Long recordId) {
        ReviewRecord record = requireRecordWritableAccess(recordId);
        if (!List.of(ReviewStatus.COMPLETED.name(), ReviewStatus.FAILED.name(), ReviewStatus.CANCELED.name())
                .contains(record.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "only terminal review records can be archived");
        }
        int updated = reviewRecordRepository.archive(recordId, SecurityUtils.getCurrentUserId());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "review record archive failed");
        }
        return getRecord(recordId);
    }

    @Transactional
    public ReviewRecordResponse updateIssue(Long recordId, String issueId, ReviewIssueUpdateRequest request) {
        ReviewRecord record = requireRecordWritableAccess(recordId);
        if (!ReviewStatus.COMPLETED.name().equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "only completed review record issues can be updated");
        }
        String normalizedIssueId = normalizeRequired(issueId, "issueId is required");
        String normalizedStatus = normalizeIssueStatus(request.getStatus());
        List<Map<String, Object>> issues = readList(record.getIssuesJson());
        boolean found = false;
        for (Map<String, Object> issue : issues) {
            if (normalizedIssueId.equals(String.valueOf(issue.get("issueId")))) {
                issue.put("status", normalizedStatus);
                issue.put("comment", trimToNull(request.getComment()));
                found = true;
            }
        }
        if (!found) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "review issue not found");
        }
        Map<String, Object> result = new LinkedHashMap<>(readMap(record.getResultJson()));
        result.put("issues", issues);
        int updated = reviewRecordRepository.updateIssues(
                recordId, writeJson(issues), writeJson(result), SecurityUtils.getCurrentUserId());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "review issue update failed");
        }
        return getRecord(recordId);
    }

    public List<ReviewRecord> findWaitingForParse(int limit) {
        return reviewRecordRepository.findWaitingForParse(Math.max(1, Math.min(limit, 100)));
    }

    @Transactional
    public void advanceParsedReview(Long recordId) {
        ReviewRecord record = reviewRecordRepository.findById(recordId).orElse(null);
        if (record == null || !ReviewStatus.PENDING.name().equals(record.getStatus())
                || !"PARSING".equals(record.getCurrentStage())) {
            return;
        }
        FileParseRecordResponse parse;
        try {
            parse = fileParseApplicationService.getParseRecordForSystem(record.getParseRecordId());
        } catch (RuntimeException ex) {
            failWaitingReview(record, "review file parse status check failed: " + limitError(ex.getMessage()));
            return;
        }
        if (FileParseStatus.SUCCESS.name().equals(parse.getStatus())) {
            try {
                requireCompleteMarkdownParse(parse);
            } catch (RuntimeException ex) {
                failWaitingReview(record, ex.getMessage());
                return;
            }
            int ready = reviewRecordRepository.markParseReady(recordId, operatorId(record));
            if (ready == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "review parse-ready state changed");
            }
            taskSubmissionApplicationService.queuePendingTask(
                    record.getTaskId(), "REVIEW_VALIDATE", operatorId(record), "review parse completed");
            return;
        }
        if (FileParseStatus.FAILED.name().equals(parse.getStatus())
                || FileParseStatus.CANCELED.name().equals(parse.getStatus())) {
            String error = limitError(parse.getErrorMessage() == null
                    ? "review file parse failed"
                    : "review file parse failed: " + parse.getErrorMessage());
            failWaitingReview(record, error);
        }
    }

    private void failWaitingReview(ReviewRecord record, String message) {
        String error = limitError(message);
        int failed = reviewRecordRepository.markFailed(record.getId(), error, operatorId(record));
        if (failed == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "review parse failure state cannot be persisted");
        }
        taskSubmissionApplicationService.failWaitingTask(
                record.getTaskId(), "REVIEW_PARSE_FAILED", error, operatorId(record));
    }

    public void executeReviewTask(Long recordId, Long taskId, String workerId) {
        executeReviewTask(recordId, taskId, workerId, 300);
    }

    public void executeReviewTask(Long recordId, Long taskId, String workerId, long leaseSeconds) {
        ReviewRecord record = requireRecordForSystem(recordId);
        if (!taskId.equals(record.getTaskId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "review task does not match record");
        }
        Long operatorId = operatorId(record);
        int processing = reviewRecordRepository.markProcessing(recordId, operatorId);
        if (processing == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "review record state is not executable");
        }
        try {
            stage(taskId, workerId, "REVIEW_VALIDATE", "review input validated");
            checkCanceled(taskId, workerId, leaseSeconds);
            FileParseRecordResponse parse = fileParseApplicationService.getParseRecordForSystem(record.getParseRecordId());
            requireCompleteMarkdownParse(parse);
            FileParseContentResponse parsedContent =
                    fileParseApplicationService.getParseContentForSystem(record.getParseRecordId());

            stage(taskId, workerId, "REVIEW_CHUNK", "splitting parsed Markdown");
            List<MarkdownReviewChunk> markdownChunks = markdownReviewChunker.split(parsedContent.getContent());
            List<ChunkContext> chunks = persistOrReuseChunks(record, markdownChunks, operatorId);
            requireUpdated(reviewRecordRepository.updateProgress(
                    recordId, "REVIEWING", 45, 0, chunks.size(), operatorId),
                    "review chunk progress update failed");

            List<ReviewRuleResult> rules = reviewRecordRepository.findRuleResults(recordId);
            if (rules.isEmpty()) {
                throw new BusinessException(ErrorCode.CONFLICT, "review rule snapshot is empty");
            }
            validateModelCallBudget(rules, chunks.size());
            int completed = (int) rules.stream()
                    .filter(rule -> ReviewRuleExecutionStatus.SUCCESS.name().equals(rule.getExecutionStatus()))
                    .count();
            for (ReviewRuleResult rule : rules) {
                if (ReviewRuleExecutionStatus.SUCCESS.name().equals(rule.getExecutionStatus())) {
                    continue;
                }
                checkCanceled(taskId, workerId, leaseSeconds);
                evaluateRule(record, rule, chunks, taskId, workerId, leaseSeconds, operatorId);
                completed++;
                int progress = 45 + (completed * 40 / Math.max(1, rules.size()));
                requireUpdated(reviewRecordRepository.updateProgress(
                        recordId, "REDUCING_RULES", progress, completed, chunks.size(), operatorId),
                        "review rule progress update failed");
            }

            stage(taskId, workerId, "REVIEW_FINAL_SUMMARY", "generating final review summary");
            checkCanceled(taskId, workerId, leaseSeconds);
            requireUpdated(reviewRecordRepository.updateProgress(
                    recordId, "SUMMARIZING", 90, completed, chunks.size(), operatorId),
                    "review summary progress update failed");
            List<ReviewRuleResult> finalRules = reviewRecordRepository.findRuleResults(recordId);
            ensureAllRulesSucceeded(finalRules);
            ReviewComplianceStatus overallStatus = calculateOverallStatus(finalRules);
            SummaryResult summaryResult = generateSummary(record, overallStatus, finalRules);
            List<Map<String, Object>> issues = buildIssues(finalRules);
            Map<String, Object> result = buildFinalResult(
                    overallStatus, summaryResult, finalRules, issues, chunks.size());
            requireUpdated(reviewRecordRepository.markCompleted(
                    recordId,
                    overallStatus.name(),
                    summaryResult.summary(),
                    writeJson(issues),
                    writeJson(result),
                    operatorId),
                    "review record complete state changed");
            stage(taskId, workerId, "REVIEW_PERSIST_RESULT", "review result persisted");
        } catch (ReviewCanceledException ex) {
            int canceled = reviewRecordRepository.markCanceled(recordId, operatorId);
            if (canceled == 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "review cancellation cannot be persisted");
            }
            throw ex;
        } catch (RuntimeException ex) {
            if (isCancellationRequestedAfterFailure(taskId, workerId, ex)) {
                int canceled = reviewRecordRepository.markCanceled(recordId, operatorId);
                if (canceled == 0) {
                    throw new BusinessException(ErrorCode.CONFLICT, "review cancellation cannot be persisted");
                }
                throw new ReviewCanceledException("review task cancellation requested");
            }
            int failed = reviewRecordRepository.markFailed(recordId, limitError(ex.getMessage()), operatorId);
            if (failed == 0) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "review record failure state cannot be persisted: " + limitError(ex.getMessage()));
            }
            throw ex;
        }
    }

    private void evaluateRule(ReviewRecord record, ReviewRuleResult rule, List<ChunkContext> chunks,
                              Long taskId, String workerId, long leaseSeconds, Long operatorId) {
        requireUpdated(reviewRecordRepository.markRuleRunning(rule.getId(), operatorId),
                "review rule running state changed");
        try {
            EnumMap<ObservationType, Integer> counts = new EnumMap<>(ObservationType.class);
            List<Map<String, Object>> effectiveEvidence = new ArrayList<>();
            Set<String> matchedRequirements = new LinkedHashSet<>();
            Set<String> missingRequirements = new LinkedHashSet<>();
            for (ChunkContext chunk : chunks) {
                checkCanceled(taskId, workerId, leaseSeconds);
                ChunkObservation observation = evaluateChunk(record, rule, chunk);
                counts.merge(observation.type(), 1, Integer::sum);
                matchedRequirements.addAll(observation.matchedRequirements());
                missingRequirements.addAll(observation.missingRequirements());
                effectiveEvidence.addAll(observation.evidence());
            }
            ModelJson reduced = reduceRule(record, rule, chunks.size(), counts,
                    matchedRequirements, missingRequirements, effectiveEvidence);
            ReviewComplianceStatus status = parseComplianceStatus(reduced.data().get("complianceStatus"));
            String reason = trimToNull(asString(reduced.data().get("reason")));
            String suggestion = trimToNull(asString(reduced.data().get("suggestion")));
            if (status != ReviewComplianceStatus.COMPLIANT && reason == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "non-compliant review rule result is missing reason");
            }
            if (status != ReviewComplianceStatus.COMPLIANT && suggestion == null) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "non-compliant review rule result is missing suggestion");
            }
            if (status == ReviewComplianceStatus.COMPLIANT && effectiveEvidence.isEmpty()) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "compliant review rule result has no supporting evidence");
            }
            BigDecimal confidence = parseConfidence(reduced.data().get("confidence"));
            requireUpdated(reviewRecordRepository.markRuleSucceeded(
                    rule.getId(),
                    status.name(),
                    reason,
                    suggestion,
                    writeJson(effectiveEvidence),
                    confidence,
                    reduced.traceId(),
                    operatorId),
                    "review rule success state changed");
        } catch (RuntimeException ex) {
            int failed = reviewRecordRepository.markRuleFailed(rule.getId(), limitError(ex.getMessage()), operatorId);
            if (failed == 0) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "review rule failure state cannot be persisted: " + limitError(ex.getMessage()));
            }
            throw ex;
        }
    }

    private ChunkObservation evaluateChunk(ReviewRecord record, ReviewRuleResult rule, ChunkContext chunk) {
        Map<String, Object> promptInput = new LinkedHashMap<>();
        promptInput.put("ruleCode", rule.getRuleCode());
        promptInput.put("ruleDescription", rule.getRuleDescription());
        promptInput.put("chunkId", chunk.entity().getId());
        promptInput.put("chunkCode", chunk.entity().getChunkCode());
        promptInput.put("location", locationOf(chunk.entity()));
        promptInput.put("markdown", chunk.content());
        String goal = """
                你是合规审查证据识别器。只判断当前Markdown文档块对指定规则提供了什么证据，不能判断整篇文档的最终结论。
                只输出一个JSON对象，不要Markdown代码块，不要解释。结构必须为：
                {"observation":"SUPPORTING_EVIDENCE|VIOLATING_EVIDENCE|PARTIAL_EVIDENCE|NO_EVIDENCE|NOT_APPLICABLE",
                 "matchedRequirements":["string"],"missingRequirements":["string"],"reason":"string",
                 "evidence":[{"quote":"必须逐字来自当前块，最多1000字","analysis":"string"}],"confidence":0.0}
                NO_EVIDENCE或NOT_APPLICABLE时evidence必须为空。禁止编造原文。
                输入：
                """ + writeJson(promptInput);
        ModelJson model = invokeJson(record.getProjectId(), goal);
        String observationValue = normalizeRequired(
                asString(model.data().get("observation")), "chunk observation is required").toUpperCase(Locale.ROOT);
        if (!ALL_OBSERVATIONS.contains(observationValue)) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "unknown chunk observation");
        }
        ObservationType type = ObservationType.valueOf(observationValue);
        List<String> matched = stringList(model.data().get("matchedRequirements"));
        List<String> missing = stringList(model.data().get("missingRequirements"));
        List<Map<String, Object>> evidence = new ArrayList<>();
        Object rawEvidence = model.data().get("evidence");
        if (rawEvidence instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> raw)) {
                    throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "chunk evidence must be object");
                }
                String quote = normalizeRequired(asString(raw.get("quote")), "chunk evidence quote is required");
                if (quote.length() > MAX_QUOTE_LENGTH) {
                    throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "chunk evidence quote is too long");
                }
                if (!containsNormalized(chunk.content(), quote)) {
                    throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                            "chunk evidence quote cannot be located in source chunk");
                }
                Map<String, Object> normalized = new LinkedHashMap<>();
                normalized.put("chunkId", chunk.entity().getId());
                normalized.put("chunkCode", chunk.entity().getChunkCode());
                normalized.put("location", locationOf(chunk.entity()));
                normalized.put("evidenceType", type.name());
                normalized.put("quote", quote);
                normalized.put("analysis", limitText(asString(raw.get("analysis")), MAX_ERROR_LENGTH));
                evidence.add(normalized);
            }
        }
        if (EVIDENCE_OBSERVATIONS.contains(type.name()) && evidence.isEmpty()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "evidence observation must include source evidence");
        }
        if (!EVIDENCE_OBSERVATIONS.contains(type.name()) && !evidence.isEmpty()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "no-evidence observation cannot include evidence");
        }
        return new ChunkObservation(type, matched, missing, evidence);
    }

    private ModelJson reduceRule(ReviewRecord record, ReviewRuleResult rule, int scannedChunkCount,
                                 Map<ObservationType, Integer> counts,
                                 Set<String> matchedRequirements,
                                 Set<String> missingRequirements,
                                 List<Map<String, Object>> evidence) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("ruleCode", rule.getRuleCode());
        input.put("ruleDescription", rule.getRuleDescription());
        input.put("scannedChunkCount", scannedChunkCount);
        Map<String, Integer> countValues = new LinkedHashMap<>();
        for (ObservationType value : ObservationType.values()) {
            countValues.put(value.name(), counts.getOrDefault(value, 0));
        }
        input.put("observationCounts", countValues);
        input.put("matchedRequirements", matchedRequirements);
        input.put("missingRequirements", missingRequirements);
        input.put("effectiveEvidenceCount", evidence.size());
        input.put("effectiveEvidenceSamples", sampleEvidenceForReduction(evidence));
        String goal = """
                你是合规审查规则汇总器。输入已经覆盖完整文档的全部块级观察，请给出这条规则唯一的最终结论。
                只输出一个JSON对象：
                {"complianceStatus":"COMPLIANT|NON_COMPLIANT|PARTIALLY_COMPLIANT",
                 "reason":"string","suggestion":"string","confidence":0.0}
                COMPLIANT必须有完整支持证据且没有冲突；明确违反或全文缺少必要内容为NON_COMPLIANT；
                只满足部分要求、证据不完整或内容矛盾为PARTIALLY_COMPLIANT。
                NON_COMPLIANT和PARTIALLY_COMPLIANT的reason、suggestion必须非空。
                不得编造输入之外的证据。
                输入：
                """ + writeJson(input);
        return invokeJson(record.getProjectId(), goal);
    }

    private List<Map<String, Object>> sampleEvidenceForReduction(List<Map<String, Object>> evidence) {
        Map<String, Integer> sampledByType = new HashMap<>();
        List<Map<String, Object>> samples = new ArrayList<>();
        for (Map<String, Object> item : evidence) {
            String evidenceType = asString(item.get("evidenceType"));
            int sampled = sampledByType.getOrDefault(evidenceType, 0);
            if (sampled >= MAX_REDUCTION_EVIDENCE_SAMPLES_PER_TYPE) {
                continue;
            }
            samples.add(item);
            sampledByType.put(evidenceType, sampled + 1);
        }
        return samples;
    }

    private void validateModelCallBudget(List<ReviewRuleResult> rules, int chunkCount) {
        long unfinishedRules = rules.stream()
                .filter(rule -> !ReviewRuleExecutionStatus.SUCCESS.name().equals(rule.getExecutionStatus()))
                .count();
        long expectedCalls = unfinishedRules * chunkCount + unfinishedRules + 1;
        long worstCaseCallsWithJsonRepair = expectedCalls * 2;
        int maxCalls = Math.max(1, reviewProperties.getMaxModelCalls());
        if (worstCaseCallsWithJsonRepair > maxCalls) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "review model call count exceeds configured limit");
        }
    }

    private SummaryResult generateSummary(ReviewRecord record, ReviewComplianceStatus overallStatus,
                                          List<ReviewRuleResult> rules) {
        List<Map<String, Object>> ruleInputs = rules.stream().map(rule -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ruleCode", rule.getRuleCode());
            item.put("ruleDescription", rule.getRuleDescription());
            item.put("complianceStatus", rule.getComplianceStatus());
            item.put("reason", rule.getReason());
            item.put("suggestion", rule.getSuggestion());
            return item;
        }).toList();
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("overallStatus", overallStatus.name());
        input.put("statistics", statistics(rules));
        input.put("ruleResults", ruleInputs);
        String goal = """
                你是合规审查总结器。总体状态和每条规则状态已经由系统确定，禁止修改。
                只输出JSON对象：{"summary":"string","keyRisks":["string"],"suggestions":["string"]}。
                摘要应准确说明符合、部分符合、不符合数量，风险和建议只能来源于规则结果。
                输入：
                """ + writeJson(input);
        ModelJson model = invokeJson(record.getProjectId(), goal);
        String summary = normalizeRequired(asString(model.data().get("summary")), "review summary is required");
        return new SummaryResult(
                limitText(summary, 10000),
                stringList(model.data().get("keyRisks")),
                stringList(model.data().get("suggestions")),
                model.traceId());
    }

    private List<ChunkContext> persistOrReuseChunks(ReviewRecord record,
                                                    List<MarkdownReviewChunk> markdownChunks,
                                                    Long operatorId) {
        Map<Integer, ReviewDocumentChunk> existing = new HashMap<>();
        for (ReviewDocumentChunk chunk : reviewRecordRepository.findChunks(record.getId())) {
            existing.put(chunk.getChunkNo(), chunk);
        }
        if (existing.size() > markdownChunks.size()) {
            throw new BusinessException(ErrorCode.CONFLICT, "persisted review chunks do not match parsed Markdown");
        }
        List<ChunkContext> contexts = new ArrayList<>(markdownChunks.size());
        for (MarkdownReviewChunk source : markdownChunks) {
            ReviewDocumentChunk entity = existing.get(source.chunkNo());
            if (entity != null) {
                if (!source.contentHash().equals(entity.getContentHash())) {
                    throw new BusinessException(ErrorCode.CONFLICT,
                            "persisted review chunk hash does not match parsed Markdown");
                }
            } else {
                String objectName = fileDerivedContentApplicationService.storeMarkdownForSystem(
                        record.getProjectId(), "REVIEW_CHUNK", record.getId(),
                        source.chunkCode(), source.content());
                entity = new ReviewDocumentChunk();
                entity.setProjectId(record.getProjectId());
                entity.setReviewRecordId(record.getId());
                entity.setParseRecordId(record.getParseRecordId());
                entity.setChunkNo(source.chunkNo());
                entity.setChunkCode(source.chunkCode());
                entity.setHeadingPath(source.headingPath());
                entity.setPageStart(source.pageStart());
                entity.setPageEnd(source.pageEnd());
                entity.setSheetName(source.sheetName());
                entity.setRowStart(source.rowStart());
                entity.setRowEnd(source.rowEnd());
                entity.setContentObjectName(objectName);
                entity.setContentHash(source.contentHash());
                entity.setCharCount(source.content().length());
                entity.setTokenCount(source.tokenCount());
                entity.setStatus("READY");
                entity.setCreatedBy(operatorId);
                entity.setUpdatedBy(operatorId);
                reviewRecordRepository.insertChunk(entity);
            }
            contexts.add(new ChunkContext(entity, source.content()));
        }
        return contexts;
    }

    private ModelJson invokeJson(Long projectId, String goal) {
        AgentInvokeRequest request = new AgentInvokeRequest();
        request.setProjectId(projectId);
        request.setGoal(goal);
        request.setTools(List.of());
        request.setParameters(Map.of(
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.1
        ));
        AgentInvokeResponse response = reviewAiGateway.invokeAgentForSystem(request);
        try {
            return new ModelJson(parseAgentResult(response), response.getProviderTraceId());
        } catch (BusinessException firstFailure) {
            request.setGoal(goal + "\n上一次输出不是合法JSON。请严格按照指定结构重新输出一个完整JSON对象。");
            AgentInvokeResponse repaired = reviewAiGateway.invokeAgentForSystem(request);
            return new ModelJson(parseAgentResult(repaired), repaired.getProviderTraceId());
        }
    }

    private Map<String, Object> parseAgentResult(AgentInvokeResponse response) {
        if (response == null || response.getResult() == null || response.getResult().isBlank()) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "review agent returned empty result");
        }
        String text = response.getResult().trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        try {
            return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "review agent result must be valid JSON");
        }
    }

    private void requireCompleteMarkdownParse(FileParseRecordResponse parse) {
        if (!FileParseStatus.SUCCESS.name().equals(parse.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "review file parse result is not ready");
        }
        if (!"MARKDOWN".equals(parse.getResultFormat())) {
            throw new BusinessException(ErrorCode.CONFLICT, "review file parse result must be Markdown");
        }
        if (parse.getMetadata() != null && !parse.getMetadata().isBlank()) {
            try {
                JsonNode metadata = objectMapper.readTree(parse.getMetadata());
                if (metadata.path("inputTruncated").asBoolean(false)) {
                    throw new BusinessException(ErrorCode.CONFLICT,
                            "review file parse input was truncated and cannot be audited");
                }
            } catch (JsonProcessingException ex) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "file parse metadata is invalid");
            }
        }
    }

    private void ensureAllRulesSucceeded(List<ReviewRuleResult> rules) {
        if (rules.isEmpty() || rules.stream().anyMatch(
                rule -> !ReviewRuleExecutionStatus.SUCCESS.name().equals(rule.getExecutionStatus())
                        || rule.getComplianceStatus() == null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "review rules are not fully completed");
        }
    }

    private ReviewComplianceStatus calculateOverallStatus(List<ReviewRuleResult> rules) {
        if (rules.stream().anyMatch(rule ->
                ReviewComplianceStatus.NON_COMPLIANT.name().equals(rule.getComplianceStatus()))) {
            return ReviewComplianceStatus.NON_COMPLIANT;
        }
        if (rules.stream().anyMatch(rule ->
                ReviewComplianceStatus.PARTIALLY_COMPLIANT.name().equals(rule.getComplianceStatus()))) {
            return ReviewComplianceStatus.PARTIALLY_COMPLIANT;
        }
        return ReviewComplianceStatus.COMPLIANT;
    }

    private Map<String, Integer> statistics(List<ReviewRuleResult> rules) {
        Map<String, Integer> values = new LinkedHashMap<>();
        values.put("total", rules.size());
        values.put("compliant", (int) rules.stream().filter(rule ->
                ReviewComplianceStatus.COMPLIANT.name().equals(rule.getComplianceStatus())).count());
        values.put("partiallyCompliant", (int) rules.stream().filter(rule ->
                ReviewComplianceStatus.PARTIALLY_COMPLIANT.name().equals(rule.getComplianceStatus())).count());
        values.put("nonCompliant", (int) rules.stream().filter(rule ->
                ReviewComplianceStatus.NON_COMPLIANT.name().equals(rule.getComplianceStatus())).count());
        return values;
    }

    private List<Map<String, Object>> buildIssues(List<ReviewRuleResult> rules) {
        List<Map<String, Object>> issues = new ArrayList<>();
        for (ReviewRuleResult rule : rules) {
            if (ReviewComplianceStatus.COMPLIANT.name().equals(rule.getComplianceStatus())) {
                continue;
            }
            List<Map<String, Object>> evidence = readList(rule.getEvidenceJson());
            Map<String, Object> issue = new LinkedHashMap<>();
            issue.put("issueId", "RULE-" + rule.getRuleCode());
            issue.put("ruleCode", rule.getRuleCode());
            issue.put("ruleName", rule.getRuleDescription());
            issue.put("complianceStatus", rule.getComplianceStatus());
            issue.put("severity", ReviewComplianceStatus.NON_COMPLIANT.name().equals(rule.getComplianceStatus())
                    ? "HIGH" : "MEDIUM");
            issue.put("location", evidence.isEmpty() ? "全文" : evidence.get(0).get("location"));
            issue.put("description", rule.getReason());
            issue.put("suggestion", rule.getSuggestion());
            issue.put("status", "OPEN");
            issues.add(issue);
        }
        return issues;
    }

    private Map<String, Object> buildFinalResult(ReviewComplianceStatus overallStatus,
                                                 SummaryResult summary,
                                                 List<ReviewRuleResult> rules,
                                                 List<Map<String, Object>> issues,
                                                 int chunkCount) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overallStatus", overallStatus.name());
        result.put("summary", summary.summary());
        result.put("statistics", statistics(rules));
        result.put("keyRisks", summary.keyRisks());
        result.put("suggestions", summary.suggestions());
        result.put("chunkCount", chunkCount);
        result.put("issues", issues);
        result.put("providerTraceId", summary.traceId());
        return result;
    }

    private Map<String, Object> locationOf(ReviewDocumentChunk chunk) {
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("headingPath", chunk.getHeadingPath());
        location.put("pageStart", chunk.getPageStart());
        location.put("pageEnd", chunk.getPageEnd());
        location.put("sheetName", chunk.getSheetName());
        location.put("rowStart", chunk.getRowStart());
        location.put("rowEnd", chunk.getRowEnd());
        return location;
    }

    private void stage(Long taskId, String workerId, String stageCode, String summary) {
        taskWorkerApplicationService.recordStage(taskId, workerId, stageCode, summary);
    }

    private void checkCanceled(Long taskId, String workerId, long leaseSeconds) {
        taskWorkerApplicationService.renewLease(taskId, workerId, leaseSeconds);
        if (isCancellationRequested(taskId, workerId)) {
            throw new ReviewCanceledException("review task cancellation requested");
        }
    }

    private boolean isCancellationRequested(Long taskId, String workerId) {
        return taskWorkerApplicationService.isCancellationRequested(taskId, workerId);
    }

    private boolean isCancellationRequestedAfterFailure(Long taskId, String workerId, RuntimeException original) {
        try {
            return isCancellationRequested(taskId, workerId);
        } catch (RuntimeException cancellationCheckFailure) {
            original.addSuppressed(cancellationCheckFailure);
            return false;
        }
    }

    private TemplateResponse requireReviewTemplate(Long projectId, Long templateId) {
        TemplateResponse template = templateApplicationService.getTemplate(templateId);
        if (!projectId.equals(template.getProjectId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "review template does not belong to project");
        }
        if (!"REVIEW".equals(template.getTemplateCategory())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "template is not a review template");
        }
        if (!"ENABLED".equals(template.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "review template is not enabled");
        }
        return template;
    }

    private List<TemplateVariableDescriptionResponse> requireReviewRules(Long templateId) {
        List<TemplateVariableDescriptionResponse> definitions =
                templateVariableApplicationService.listDescriptions(templateId);
        if (definitions.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT, "review template has no parsed review rules");
        }
        Set<String> codes = new LinkedHashSet<>();
        for (TemplateVariableDescriptionResponse definition : definitions) {
            if (definition.getVariableName() == null || definition.getVariableName().isBlank()
                    || definition.getDescription() == null || definition.getDescription().isBlank()) {
                throw new BusinessException(ErrorCode.CONFLICT, "review template contains an incomplete review rule");
            }
            if (!codes.add(definition.getVariableName())) {
                throw new BusinessException(ErrorCode.CONFLICT, "review template contains duplicate rule codes");
            }
        }
        return definitions;
    }

    private void validateReviewFile(ReviewSubmitRequest request) {
        if (request.getFile() == null || request.getFile().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "review file is required");
        }
        String filename = request.getFile().getOriginalFilename();
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "review file extension is required");
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!REVIEW_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "review file must be DOC, DOCX, PDF, TXT, XLS or XLSX");
        }
    }

    private ReviewRecord requireRecordAccess(Long recordId) {
        ReviewRecord record = requireRecord(recordId);
        projectAccessApplicationService.requireProjectAccess(record.getProjectId());
        return record;
    }

    private ReviewRecord requireRecordWritableAccess(Long recordId) {
        ReviewRecord record = requireRecordAccess(recordId);
        projectAccessApplicationService.requireProjectWritableAccess(record.getProjectId());
        return record;
    }

    private ReviewRecord requireRecordForSystem(Long recordId) {
        ReviewRecord record = requireRecord(recordId);
        projectAccessApplicationService.requireProjectWritableForSystem(record.getProjectId());
        return record;
    }

    private ReviewRecord requireRecord(Long recordId) {
        if (recordId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "recordId is required");
        }
        return reviewRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "review record not found"));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ReviewStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "status must be PENDING, PROCESSING, COMPLETED, FAILED, CANCELED or ARCHIVED");
        }
    }

    private String normalizeIssueStatus(String status) {
        String normalized = normalizeRequired(status, "status is required").toUpperCase(Locale.ROOT);
        if (!List.of("OPEN", "PROCESSING", "RESOLVED", "IGNORED").contains(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "issue status must be OPEN, PROCESSING, RESOLVED or IGNORED");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ReviewRecordResponse toResponse(ReviewRecord record) {
        ReviewRecordResponse response = new ReviewRecordResponse();
        response.setRecordId(record.getId());
        response.setProjectId(record.getProjectId());
        response.setTemplateId(record.getTemplateId());
        response.setFileId(record.getFileId());
        response.setTaskId(record.getTaskId());
        response.setParseRecordId(record.getParseRecordId());
        response.setStatus(record.getStatus());
        response.setCurrentStage(record.getCurrentStage());
        response.setProgress(record.getProgress());
        response.setOverallStatus(record.getOverallStatus());
        response.setSummary(record.getSummary());
        response.setRuleTotal(record.getRuleTotal());
        response.setRuleCompleted(record.getRuleCompleted());
        response.setChunkTotal(record.getChunkTotal());
        response.setIssues(readList(record.getIssuesJson()));
        response.setResult(readMap(record.getResultJson()));
        response.setErrorMessage(record.getErrorMessage());
        response.setStartedAt(record.getStartedAt());
        response.setCompletedAt(record.getCompletedAt());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        return response;
    }

    private ReviewRuleResultResponse toRuleResponse(ReviewRuleResult rule) {
        ReviewRuleResultResponse response = new ReviewRuleResultResponse();
        response.setRuleResultId(rule.getId());
        response.setRuleCode(rule.getRuleCode());
        response.setRuleDescription(rule.getRuleDescription());
        response.setRuleOrder(rule.getRuleOrder());
        response.setExecutionStatus(rule.getExecutionStatus());
        response.setComplianceStatus(rule.getComplianceStatus());
        response.setReason(rule.getReason());
        response.setSuggestion(rule.getSuggestion());
        response.setEvidence(readList(rule.getEvidenceJson()));
        response.setConfidence(rule.getConfidence());
        response.setModelTraceId(rule.getModelTraceId());
        response.setErrorMessage(rule.getErrorMessage());
        response.setStartedAt(rule.getStartedAt());
        response.setCompletedAt(rule.getCompletedAt());
        return response;
    }

    private ReviewComplianceStatus parseComplianceStatus(Object value) {
        try {
            return ReviewComplianceStatus.valueOf(normalizeRequired(
                    asString(value), "complianceStatus is required").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "unknown compliance status");
        }
    }

    private BigDecimal parseConfidence(Object value) {
        if (!(value instanceof Number) && (value == null || asString(value).isBlank())) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "review confidence is required");
        }
        try {
            BigDecimal confidence = new BigDecimal(asString(value)).setScale(4, RoundingMode.HALF_UP);
            if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
                throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "review confidence must be between 0 and 1");
            }
            return confidence;
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "review confidence must be numeric");
        }
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(this::asString).filter(item -> !item.isBlank()).map(String::trim).toList();
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean containsNormalized(String source, String quote) {
        return normalizeWhitespace(source).contains(normalizeWhitespace(quote));
    }

    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "review json serialization failed");
        }
    }

    private List<Map<String, Object>> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "review list json parse failed");
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "review result json parse failed");
        }
    }

    private String limitError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "review execution failed";
        }
        return limitText(errorMessage.trim(), MAX_ERROR_LENGTH);
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private Long operatorId(ReviewRecord record) {
        return record.getCreatedBy() == null ? 1L : record.getCreatedBy();
    }

    private void requireUpdated(int updated, String message) {
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, message);
        }
    }

    private enum ObservationType {
        SUPPORTING_EVIDENCE,
        VIOLATING_EVIDENCE,
        PARTIAL_EVIDENCE,
        NO_EVIDENCE,
        NOT_APPLICABLE
    }

    private record ChunkContext(ReviewDocumentChunk entity, String content) {}
    private record ChunkObservation(ObservationType type, List<String> matchedRequirements,
                                    List<String> missingRequirements,
                                    List<Map<String, Object>> evidence) {}
    private record ModelJson(Map<String, Object> data, String traceId) {}
    private record SummaryResult(String summary, List<String> keyRisks,
                                 List<String> suggestions, String traceId) {}

    private static class ReviewCanceledException extends BusinessException {
        ReviewCanceledException(String message) {
            super(ErrorCode.CONFLICT, message);
        }
    }
}
