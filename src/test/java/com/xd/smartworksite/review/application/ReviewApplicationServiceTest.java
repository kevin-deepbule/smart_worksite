package com.xd.smartworksite.review.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.ai.dto.AgentInvokeRequest;
import com.xd.smartworksite.ai.dto.AgentInvokeResponse;
import com.xd.smartworksite.common.security.UserPrincipal;
import com.xd.smartworksite.file.application.FileDerivedContentApplicationService;
import com.xd.smartworksite.file.application.FileObjectApplicationService;
import com.xd.smartworksite.file.application.FileParseApplicationService;
import com.xd.smartworksite.file.dto.FileObjectResponse;
import com.xd.smartworksite.file.dto.FileParseContentResponse;
import com.xd.smartworksite.file.dto.FileParseRecordResponse;
import com.xd.smartworksite.file.dto.FileUploadRequest;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.review.domain.ReviewDocumentChunk;
import com.xd.smartworksite.review.domain.ReviewRecord;
import com.xd.smartworksite.review.domain.ReviewRuleResult;
import com.xd.smartworksite.review.dto.ReviewSubmitRequest;
import com.xd.smartworksite.review.infra.MarkdownReviewChunker;
import com.xd.smartworksite.review.repository.ReviewRecordRepository;
import com.xd.smartworksite.task.application.TaskApplicationService;
import com.xd.smartworksite.task.application.TaskSubmissionApplicationService;
import com.xd.smartworksite.task.application.TaskWorkerApplicationService;
import com.xd.smartworksite.template.application.TemplateApplicationService;
import com.xd.smartworksite.template.application.TemplateVariableApplicationService;
import com.xd.smartworksite.template.dto.TemplateResponse;
import com.xd.smartworksite.template.dto.TemplateVariableDescriptionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewApplicationServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ReviewRecordRepository repository;
    private ProjectAccessApplicationService projectAccess;
    private FileObjectApplicationService fileObjectService;
    private FileParseApplicationService fileParseService;
    private FileDerivedContentApplicationService derivedContentService;
    private TemplateApplicationService templateService;
    private TemplateVariableApplicationService variableService;
    private ReviewSubmissionPersistenceService submissionPersistenceService;
    private ReviewRetryPersistenceService retryPersistenceService;
    private StubReviewAiGateway aiGateway;
    private TaskApplicationService taskApplicationService;
    private TaskSubmissionApplicationService taskSubmissionService;
    private TaskWorkerApplicationService taskWorkerService;
    private ReviewApplicationService service;

    @BeforeEach
    void setUp() {
        setCurrentUser();
        repository = mock(ReviewRecordRepository.class);
        projectAccess = mock(ProjectAccessApplicationService.class);
        fileObjectService = mock(FileObjectApplicationService.class);
        fileParseService = mock(FileParseApplicationService.class);
        derivedContentService = mock(FileDerivedContentApplicationService.class);
        templateService = mock(TemplateApplicationService.class);
        variableService = mock(TemplateVariableApplicationService.class);
        submissionPersistenceService = mock(ReviewSubmissionPersistenceService.class);
        retryPersistenceService = mock(ReviewRetryPersistenceService.class);
        aiGateway = new StubReviewAiGateway();
        taskApplicationService = mock(TaskApplicationService.class);
        taskSubmissionService = mock(TaskSubmissionApplicationService.class);
        taskWorkerService = mock(TaskWorkerApplicationService.class);
        ReviewProperties properties = new ReviewProperties();
        properties.getChunk().setMaxChars(1000);
        service = new ReviewApplicationService(
                repository,
                projectAccess,
                fileObjectService,
                fileParseService,
                derivedContentService,
                templateService,
                variableService,
                submissionPersistenceService,
                retryPersistenceService,
                aiGateway,
                new MarkdownReviewChunker(properties),
                properties,
                taskApplicationService,
                taskSubmissionService,
                taskWorkerService,
                objectMapper
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitCreatesParseAndReturnsPendingReviewWithoutCallingModel() {
        TemplateResponse template = template();
        when(templateService.getTemplate(10L)).thenReturn(template);
        when(variableService.listDescriptions(10L)).thenReturn(List.of(
                new TemplateVariableDescriptionResponse("var_guardrail", "检查临边防护是否完整")));
        FileObjectResponse file = new FileObjectResponse();
        file.setFileId(99L);
        file.setProjectId(1L);
        when(fileObjectService.upload(any(FileUploadRequest.class))).thenReturn(file);
        FileParseRecordResponse parse = parseRecord("PENDING");
        when(fileParseService.createParse(eq(99L), any())).thenReturn(parse);
        ReviewRecord pending = reviewRecord("PENDING", "PARSING");
        when(submissionPersistenceService.create(
                eq(1L), eq(10L), eq(88L), eq(99L), eq(77L), any(), eq(2L)))
                .thenReturn(pending);

        var response = service.submitReview(submitRequest());

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getCurrentStage()).isEqualTo("PARSING");
        assertThat(aiGateway.callCount).isZero();
    }

    @Test
    void submitCleansUploadedFileWhenParseCreationFails() {
        when(templateService.getTemplate(10L)).thenReturn(template());
        when(variableService.listDescriptions(10L)).thenReturn(List.of(
                new TemplateVariableDescriptionResponse("var_guardrail", "检查临边防护是否完整")));
        FileObjectResponse file = new FileObjectResponse();
        file.setFileId(99L);
        file.setProjectId(1L);
        when(fileObjectService.upload(any(FileUploadRequest.class))).thenReturn(file);
        when(fileParseService.createParse(eq(99L), any())).thenThrow(new IllegalStateException("parse unavailable"));

        assertThatThrownBy(() -> service.submitReview(submitRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("parse unavailable");

        verify(fileObjectService).deleteFile(99L);
    }

    @Test
    void completedParseQueuesPendingReviewTask() {
        ReviewRecord record = reviewRecord("PENDING", "PARSING");
        when(repository.findById(1L)).thenReturn(Optional.of(record));
        when(fileParseService.getParseRecordForSystem(77L)).thenReturn(parseRecord("SUCCESS"));
        when(repository.markParseReady(1L, 2L)).thenReturn(1);

        service.advanceParsedReview(1L);

        verify(taskSubmissionService).queuePendingTask(66L, "REVIEW_VALIDATE", 2L,
                "review parse completed");
    }

    @Test
    void deleteRejectsRunningReviewSoItsTaskIsNotOrphaned() {
        when(repository.findById(1L)).thenReturn(Optional.of(reviewRecord("PROCESSING", "REVIEWING")));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(com.xd.smartworksite.common.exception.BusinessException.class)
                .hasMessageContaining("canceled before deletion");

        verify(repository, never()).softDelete(eq(1L), anyLong());
    }

    @Test
    void workerPersistsSupportingEvidenceAndCompliantFinalResult() {
        ReviewRecord record = reviewRecord("PENDING", "QUEUED");
        when(repository.findById(1L)).thenReturn(Optional.of(record));
        when(repository.markProcessing(1L, 2L)).thenReturn(1);
        when(fileParseService.getParseRecordForSystem(77L)).thenReturn(parseRecord("SUCCESS"));
        FileParseContentResponse content = new FileParseContentResponse();
        content.setRecordId(77L);
        content.setResultFormat("MARKDOWN");
        content.setContent("# 安全措施\n临边区域设置了连续防护栏杆。");
        when(fileParseService.getParseContentForSystem(77L)).thenReturn(content);
        when(derivedContentService.storeMarkdownForSystem(
                eq(1L), eq("REVIEW_CHUNK"), eq(1L), anyString(), anyString()))
                .thenReturn("projects/1/DERIVED/REVIEW_CHUNK/1/CHUNK_0001.md");
        when(repository.insertChunk(any())).thenAnswer(invocation -> {
            ReviewDocumentChunk chunk = invocation.getArgument(0);
            chunk.setId(101L);
            return chunk;
        });
        when(repository.updateProgress(eq(1L), anyString(), anyInt(), any(), any(), eq(2L)))
                .thenReturn(1);
        ReviewRuleResult pendingRule = rule("PENDING", null);
        ReviewRuleResult completedRule = rule("SUCCESS", "COMPLIANT");
        completedRule.setReason("防护栏杆要求有明确证据");
        completedRule.setEvidenceJson("[{\"chunkId\":101}]");
        when(repository.findRuleResults(1L))
                .thenReturn(List.of(pendingRule))
                .thenReturn(List.of(completedRule));
        when(repository.markRuleRunning(201L, 2L)).thenReturn(1);
        when(repository.markRuleSucceeded(eq(201L), eq("COMPLIANT"), any(), any(),
                anyString(), any(BigDecimal.class), anyString(), eq(2L))).thenReturn(1);
        when(repository.markCompleted(eq(1L), eq("COMPLIANT"), anyString(),
                anyString(), anyString(), eq(2L))).thenReturn(1);
        when(taskWorkerService.isCancellationRequested(66L, "worker-1")).thenReturn(false);

        service.executeReviewTask(1L, 66L, "worker-1");

        ArgumentCaptor<String> evidenceCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).markRuleSucceeded(eq(201L), eq("COMPLIANT"), any(), any(),
                evidenceCaptor.capture(), any(BigDecimal.class), anyString(), eq(2L));
        assertThat(evidenceCaptor.getValue())
                .contains("\"chunkId\":101")
                .contains("临边区域设置了连续防护栏杆");
        verify(repository).markCompleted(eq(1L), eq("COMPLIANT"), anyString(),
                eq("[]"), anyString(), eq(2L));
    }

    private ReviewSubmitRequest submitRequest() {
        ReviewSubmitRequest request = new ReviewSubmitRequest();
        request.setProjectId(1L);
        request.setTemplateId(10L);
        request.setFile(new MockMultipartFile(
                "file", "plan.pdf", "application/pdf", "content".getBytes()));
        return request;
    }

    private TemplateResponse template() {
        TemplateResponse response = new TemplateResponse();
        response.setTemplateId(10L);
        response.setProjectId(1L);
        response.setFileId(88L);
        response.setTemplateCategory("REVIEW");
        response.setStatus("ENABLED");
        return response;
    }

    private FileParseRecordResponse parseRecord(String status) {
        FileParseRecordResponse response = new FileParseRecordResponse();
        response.setRecordId(77L);
        response.setProjectId(1L);
        response.setFileId(99L);
        response.setStatus(status);
        response.setResultFormat("MARKDOWN");
        response.setMetadata("{\"inputTruncated\":false}");
        return response;
    }

    private ReviewRecord reviewRecord(String status, String stage) {
        ReviewRecord record = new ReviewRecord();
        record.setId(1L);
        record.setProjectId(1L);
        record.setTemplateId(10L);
        record.setFileId(99L);
        record.setParseRecordId(77L);
        record.setTaskId(66L);
        record.setStatus(status);
        record.setCurrentStage(stage);
        record.setProgress(5);
        record.setRuleTotal(1);
        record.setRuleCompleted(0);
        record.setChunkTotal(0);
        record.setIssuesJson("[]");
        record.setResultJson("{}");
        record.setCreatedBy(2L);
        return record;
    }

    private ReviewRuleResult rule(String executionStatus, String complianceStatus) {
        ReviewRuleResult rule = new ReviewRuleResult();
        rule.setId(201L);
        rule.setProjectId(1L);
        rule.setReviewRecordId(1L);
        rule.setTemplateId(10L);
        rule.setRuleCode("var_guardrail");
        rule.setRuleDescription("检查临边防护是否完整");
        rule.setRuleOrder(1);
        rule.setExecutionStatus(executionStatus);
        rule.setComplianceStatus(complianceStatus);
        return rule;
    }

    private void setCurrentUser() {
        UserPrincipal principal = new UserPrincipal(
                2L, "review-user", List.of("BUSINESS_USER"), List.of("review:view", "review:manage"), 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static class StubReviewAiGateway implements ReviewAiGateway {
        private int callCount;

        @Override
        public AgentInvokeResponse invokeAgent(AgentInvokeRequest request) {
            return invokeAgentForSystem(request);
        }

        @Override
        public AgentInvokeResponse invokeAgentForSystem(AgentInvokeRequest request) {
            callCount++;
            AgentInvokeResponse response = new AgentInvokeResponse();
            response.setProviderTraceId("trace-" + callCount);
            if (request.getGoal().contains("合规审查证据识别器")) {
                response.setResult("""
                        {"observation":"SUPPORTING_EVIDENCE",
                         "matchedRequirements":["连续防护栏杆"],"missingRequirements":[],
                         "reason":"发现明确措施",
                         "evidence":[{"quote":"临边区域设置了连续防护栏杆","analysis":"支持规则"}],
                         "confidence":0.95}
                        """);
            } else if (request.getGoal().contains("合规审查规则汇总器")) {
                response.setResult("""
                        {"complianceStatus":"COMPLIANT","reason":"防护栏杆要求有明确证据",
                         "suggestion":"","confidence":0.94}
                        """);
            } else {
                response.setResult("""
                        {"summary":"共1条规则，1条符合。","keyRisks":[],"suggestions":[]}
                        """);
            }
            return response;
        }
    }
}
