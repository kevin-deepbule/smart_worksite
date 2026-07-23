package com.xd.smartworksite.review.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.review.domain.ReviewRecord;
import com.xd.smartworksite.review.domain.ReviewRuleExecutionStatus;
import com.xd.smartworksite.review.domain.ReviewRuleResult;
import com.xd.smartworksite.review.domain.ReviewStatus;
import com.xd.smartworksite.review.repository.ReviewRecordRepository;
import com.xd.smartworksite.task.application.TaskSubmissionApplicationService;
import com.xd.smartworksite.task.domain.GenerateTask;
import com.xd.smartworksite.template.dto.TemplateVariableDescriptionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewSubmissionPersistenceService {
    private static final String TASK_TYPE = "REVIEW_EXECUTION";
    private static final String BIZ_TYPE = "REVIEW";

    private final ReviewRecordRepository reviewRecordRepository;
    private final TaskSubmissionApplicationService taskSubmissionApplicationService;

    public ReviewSubmissionPersistenceService(ReviewRecordRepository reviewRecordRepository,
                                              TaskSubmissionApplicationService taskSubmissionApplicationService) {
        this.reviewRecordRepository = reviewRecordRepository;
        this.taskSubmissionApplicationService = taskSubmissionApplicationService;
    }

    @Transactional
    public ReviewRecord create(Long projectId, Long templateId, Long templateFileId,
                               Long fileId, Long parseRecordId,
                               List<TemplateVariableDescriptionResponse> rules,
                               Long operatorId) {
        ReviewRecord record = new ReviewRecord();
        record.setProjectId(projectId);
        record.setTemplateId(templateId);
        record.setFileId(fileId);
        record.setParseRecordId(parseRecordId);
        record.setStatus(ReviewStatus.PENDING.name());
        record.setCurrentStage("PARSING");
        record.setProgress(5);
        record.setRuleTotal(rules.size());
        record.setRuleCompleted(0);
        record.setChunkTotal(0);
        record.setIssuesJson("[]");
        record.setResultJson("{}");
        record.setCreatedBy(operatorId);
        record.setUpdatedBy(operatorId);
        reviewRecordRepository.insert(record);
        if (record.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "review record id was not generated");
        }

        for (int index = 0; index < rules.size(); index++) {
            TemplateVariableDescriptionResponse definition = rules.get(index);
            ReviewRuleResult result = new ReviewRuleResult();
            result.setProjectId(projectId);
            result.setReviewRecordId(record.getId());
            result.setTemplateId(templateId);
            result.setTemplateFileId(templateFileId);
            result.setRuleCode(definition.getVariableName());
            result.setRuleDescription(definition.getDescription());
            result.setRuleOrder(index + 1);
            result.setExecutionStatus(ReviewRuleExecutionStatus.PENDING.name());
            result.setEvidenceJson("[]");
            result.setCreatedBy(operatorId);
            result.setUpdatedBy(operatorId);
            reviewRecordRepository.insertRuleResult(result);
        }

        GenerateTask task = taskSubmissionApplicationService.createPendingTask(
                projectId, TASK_TYPE, BIZ_TYPE, record.getId(), "REVIEW_PARSE_WAIT", 3);
        int linked = reviewRecordRepository.linkTask(record.getId(), task.getId(), operatorId);
        if (linked == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "review task link update failed");
        }
        return reviewRecordRepository.findById(record.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_ERROR, "review record is not readable"));
    }
}
