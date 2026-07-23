package com.xd.smartworksite.review.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.review.repository.ReviewRecordRepository;
import com.xd.smartworksite.task.application.TaskSubmissionApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewRetryPersistenceService {
    private final ReviewRecordRepository reviewRecordRepository;
    private final TaskSubmissionApplicationService taskSubmissionApplicationService;

    public ReviewRetryPersistenceService(ReviewRecordRepository reviewRecordRepository,
                                         TaskSubmissionApplicationService taskSubmissionApplicationService) {
        this.reviewRecordRepository = reviewRecordRepository;
        this.taskSubmissionApplicationService = taskSubmissionApplicationService;
    }

    @Transactional
    public void resetToParseWaiting(Long recordId, Long parseRecordId, Long taskId, Long operatorId) {
        int reset = reviewRecordRepository.resetForParseRetry(recordId, parseRecordId, operatorId);
        if (reset == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "review parse retry state changed");
        }
        taskSubmissionApplicationService.resetFailedTaskToPending(
                taskId, "REVIEW_PARSE_WAIT", operatorId);
    }
}
