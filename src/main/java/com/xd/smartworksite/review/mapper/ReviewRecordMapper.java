package com.xd.smartworksite.review.mapper;

import com.xd.smartworksite.review.domain.ReviewRecord;
import com.xd.smartworksite.review.domain.ReviewDocumentChunk;
import com.xd.smartworksite.review.domain.ReviewRuleResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReviewRecordMapper {
    int insert(ReviewRecord record);

    ReviewRecord selectById(@Param("recordId") Long recordId);

    List<ReviewRecord> selectPage(@Param("projectId") Long projectId,
                                  @Param("accessibleProjectIds") List<Long> accessibleProjectIds,
                                  @Param("templateId") Long templateId,
                                  @Param("status") String status);

    List<ReviewRecord> selectWaitingForParse(@Param("limit") int limit);

    int linkTask(@Param("recordId") Long recordId,
                 @Param("taskId") Long taskId,
                 @Param("updatedBy") Long updatedBy);

    int markParseReady(@Param("recordId") Long recordId,
                       @Param("updatedBy") Long updatedBy);

    int markProcessing(@Param("recordId") Long recordId,
                       @Param("updatedBy") Long updatedBy);

    int markCompleted(@Param("recordId") Long recordId,
                      @Param("overallStatus") String overallStatus,
                      @Param("summary") String summary,
                      @Param("issuesJson") String issuesJson,
                      @Param("resultJson") String resultJson,
                      @Param("updatedBy") Long updatedBy);

    int updateProgress(@Param("recordId") Long recordId,
                       @Param("currentStage") String currentStage,
                       @Param("progress") int progress,
                       @Param("ruleCompleted") Integer ruleCompleted,
                       @Param("chunkTotal") Integer chunkTotal,
                       @Param("updatedBy") Long updatedBy);

    int resetForParseRetry(@Param("recordId") Long recordId,
                           @Param("parseRecordId") Long parseRecordId,
                           @Param("updatedBy") Long updatedBy);

    int updateIssues(@Param("recordId") Long recordId,
                     @Param("issuesJson") String issuesJson,
                     @Param("resultJson") String resultJson,
                     @Param("updatedBy") Long updatedBy);

    int markFailed(@Param("recordId") Long recordId,
                   @Param("errorMessage") String errorMessage,
                   @Param("updatedBy") Long updatedBy);

    int markCanceled(@Param("recordId") Long recordId,
                     @Param("updatedBy") Long updatedBy);

    int softDelete(@Param("recordId") Long recordId,
                   @Param("updatedBy") Long updatedBy);

    int archive(@Param("recordId") Long recordId,
                @Param("updatedBy") Long updatedBy);

    int insertRuleResult(ReviewRuleResult result);

    List<ReviewRuleResult> selectRuleResults(@Param("recordId") Long recordId);

    ReviewRuleResult selectRuleResult(@Param("recordId") Long recordId,
                                      @Param("ruleCode") String ruleCode);

    int markRuleRunning(@Param("ruleResultId") Long ruleResultId,
                        @Param("updatedBy") Long updatedBy);

    int markRuleSucceeded(@Param("ruleResultId") Long ruleResultId,
                          @Param("complianceStatus") String complianceStatus,
                          @Param("reason") String reason,
                          @Param("suggestion") String suggestion,
                          @Param("evidenceJson") String evidenceJson,
                          @Param("confidence") java.math.BigDecimal confidence,
                          @Param("modelTraceId") String modelTraceId,
                          @Param("updatedBy") Long updatedBy);

    int markRuleFailed(@Param("ruleResultId") Long ruleResultId,
                       @Param("errorMessage") String errorMessage,
                       @Param("updatedBy") Long updatedBy);

    int insertChunk(ReviewDocumentChunk chunk);

    List<ReviewDocumentChunk> selectChunks(@Param("recordId") Long recordId);

    int softDeleteRules(@Param("recordId") Long recordId,
                        @Param("updatedBy") Long updatedBy);

    int softDeleteChunks(@Param("recordId") Long recordId,
                         @Param("updatedBy") Long updatedBy);
}
