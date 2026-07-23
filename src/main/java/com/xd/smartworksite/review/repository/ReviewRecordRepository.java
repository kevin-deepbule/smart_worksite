package com.xd.smartworksite.review.repository;

import com.xd.smartworksite.review.domain.ReviewRecord;
import com.xd.smartworksite.review.domain.ReviewDocumentChunk;
import com.xd.smartworksite.review.domain.ReviewRuleResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ReviewRecordRepository {
    ReviewRecord insert(ReviewRecord record);

    Optional<ReviewRecord> findById(Long recordId);

    List<ReviewRecord> findPage(Long projectId, List<Long> accessibleProjectIds, Long templateId, String status);

    List<ReviewRecord> findWaitingForParse(int limit);

    int linkTask(Long recordId, Long taskId, Long updatedBy);

    int markParseReady(Long recordId, Long updatedBy);

    int markProcessing(Long recordId, Long updatedBy);

    int markCompleted(Long recordId, String overallStatus, String summary,
                      String issuesJson, String resultJson, Long updatedBy);

    int updateProgress(Long recordId, String currentStage, int progress,
                       Integer ruleCompleted, Integer chunkTotal, Long updatedBy);

    int resetForParseRetry(Long recordId, Long parseRecordId, Long updatedBy);

    int updateIssues(Long recordId, String issuesJson, String resultJson, Long updatedBy);

    int markFailed(Long recordId, String errorMessage, Long updatedBy);

    int markCanceled(Long recordId, Long updatedBy);

    int softDelete(Long recordId, Long updatedBy);

    int archive(Long recordId, Long updatedBy);

    ReviewRuleResult insertRuleResult(ReviewRuleResult result);

    List<ReviewRuleResult> findRuleResults(Long recordId);

    Optional<ReviewRuleResult> findRuleResult(Long recordId, String ruleCode);

    int markRuleRunning(Long ruleResultId, Long updatedBy);

    int markRuleSucceeded(Long ruleResultId, String complianceStatus, String reason,
                          String suggestion, String evidenceJson, BigDecimal confidence,
                          String modelTraceId, Long updatedBy);

    int markRuleFailed(Long ruleResultId, String errorMessage, Long updatedBy);

    ReviewDocumentChunk insertChunk(ReviewDocumentChunk chunk);

    List<ReviewDocumentChunk> findChunks(Long recordId);

    int softDeleteRules(Long recordId, Long updatedBy);

    int softDeleteChunks(Long recordId, Long updatedBy);
}
