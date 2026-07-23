package com.xd.smartworksite.review.application;

import com.xd.smartworksite.review.domain.ReviewRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReviewParseCoordinator {
    private static final Logger log = LoggerFactory.getLogger(ReviewParseCoordinator.class);

    private final ReviewApplicationService reviewApplicationService;
    private final ReviewProperties reviewProperties;

    public ReviewParseCoordinator(ReviewApplicationService reviewApplicationService,
                                  ReviewProperties reviewProperties) {
        this.reviewApplicationService = reviewApplicationService;
        this.reviewProperties = reviewProperties;
    }

    @Scheduled(fixedDelayString = "${app.review.parse-poll-delay-ms:2000}")
    public void advanceWaitingReviews() {
        for (ReviewRecord record : reviewApplicationService.findWaitingForParse(
                reviewProperties.getParsePollLimit())) {
            try {
                reviewApplicationService.advanceParsedReview(record.getId());
            } catch (RuntimeException ex) {
                log.error("advance parsed review failed, recordId={}, parseRecordId={}",
                        record.getId(), record.getParseRecordId(), ex);
            }
        }
    }
}
