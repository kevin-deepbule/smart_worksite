package com.xd.smartworksite.review.repository;

import com.xd.smartworksite.review.domain.ReviewRecord;
import com.xd.smartworksite.review.domain.ReviewDocumentChunk;
import com.xd.smartworksite.review.domain.ReviewRuleResult;
import com.xd.smartworksite.review.mapper.ReviewRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public class MyBatisReviewRecordRepository implements ReviewRecordRepository {
    private final ReviewRecordMapper mapper;

    public MyBatisReviewRecordRepository(ReviewRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ReviewRecord insert(ReviewRecord record) {
        int inserted = mapper.insert(record);
        if (inserted <= 0 || record.getId() == null) {
            throw new IllegalStateException("review record insert failed or id was not generated");
        }
        return record;
    }

    @Override
    public Optional<ReviewRecord> findById(Long recordId) {
        return Optional.ofNullable(mapper.selectById(recordId));
    }

    @Override
    public List<ReviewRecord> findPage(Long projectId, List<Long> accessibleProjectIds, Long templateId, String status) {
        return mapper.selectPage(projectId, accessibleProjectIds, templateId, status);
    }

    @Override
    public List<ReviewRecord> findWaitingForParse(int limit) {
        return mapper.selectWaitingForParse(limit);
    }

    @Override
    public int linkTask(Long recordId, Long taskId, Long updatedBy) {
        return mapper.linkTask(recordId, taskId, updatedBy);
    }

    @Override
    public int markParseReady(Long recordId, Long updatedBy) {
        return mapper.markParseReady(recordId, updatedBy);
    }

    @Override
    public int markProcessing(Long recordId, Long updatedBy) {
        return mapper.markProcessing(recordId, updatedBy);
    }

    @Override
    public int markCompleted(Long recordId, String overallStatus, String summary,
                             String issuesJson, String resultJson, Long updatedBy) {
        return mapper.markCompleted(recordId, overallStatus, summary, issuesJson, resultJson, updatedBy);
    }

    @Override
    public int updateProgress(Long recordId, String currentStage, int progress,
                              Integer ruleCompleted, Integer chunkTotal, Long updatedBy) {
        return mapper.updateProgress(recordId, currentStage, progress, ruleCompleted, chunkTotal, updatedBy);
    }

    @Override
    public int resetForParseRetry(Long recordId, Long parseRecordId, Long updatedBy) {
        return mapper.resetForParseRetry(recordId, parseRecordId, updatedBy);
    }

    @Override
    public int updateIssues(Long recordId, String issuesJson, String resultJson, Long updatedBy) {
        return mapper.updateIssues(recordId, issuesJson, resultJson, updatedBy);
    }

    @Override
    public int markFailed(Long recordId, String errorMessage, Long updatedBy) {
        return mapper.markFailed(recordId, errorMessage, updatedBy);
    }

    @Override
    public int markCanceled(Long recordId, Long updatedBy) {
        return mapper.markCanceled(recordId, updatedBy);
    }

    @Override
    public int softDelete(Long recordId, Long updatedBy) {
        return mapper.softDelete(recordId, updatedBy);
    }

    @Override
    public int archive(Long recordId, Long updatedBy) {
        return mapper.archive(recordId, updatedBy);
    }

    @Override
    public ReviewRuleResult insertRuleResult(ReviewRuleResult result) {
        int inserted = mapper.insertRuleResult(result);
        if (inserted <= 0 || result.getId() == null) {
            throw new IllegalStateException("review rule result insert failed or id was not generated");
        }
        return result;
    }

    @Override
    public List<ReviewRuleResult> findRuleResults(Long recordId) {
        return mapper.selectRuleResults(recordId);
    }

    @Override
    public Optional<ReviewRuleResult> findRuleResult(Long recordId, String ruleCode) {
        return Optional.ofNullable(mapper.selectRuleResult(recordId, ruleCode));
    }

    @Override
    public int markRuleRunning(Long ruleResultId, Long updatedBy) {
        return mapper.markRuleRunning(ruleResultId, updatedBy);
    }

    @Override
    public int markRuleSucceeded(Long ruleResultId, String complianceStatus, String reason,
                                 String suggestion, String evidenceJson, BigDecimal confidence,
                                 String modelTraceId, Long updatedBy) {
        return mapper.markRuleSucceeded(ruleResultId, complianceStatus, reason, suggestion,
                evidenceJson, confidence, modelTraceId, updatedBy);
    }

    @Override
    public int markRuleFailed(Long ruleResultId, String errorMessage, Long updatedBy) {
        return mapper.markRuleFailed(ruleResultId, errorMessage, updatedBy);
    }

    @Override
    public ReviewDocumentChunk insertChunk(ReviewDocumentChunk chunk) {
        int inserted = mapper.insertChunk(chunk);
        if (inserted <= 0 || chunk.getId() == null) {
            throw new IllegalStateException("review document chunk insert failed or id was not generated");
        }
        return chunk;
    }

    @Override
    public List<ReviewDocumentChunk> findChunks(Long recordId) {
        return mapper.selectChunks(recordId);
    }

    @Override
    public int softDeleteRules(Long recordId, Long updatedBy) {
        return mapper.softDeleteRules(recordId, updatedBy);
    }

    @Override
    public int softDeleteChunks(Long recordId, Long updatedBy) {
        return mapper.softDeleteChunks(recordId, updatedBy);
    }
}
