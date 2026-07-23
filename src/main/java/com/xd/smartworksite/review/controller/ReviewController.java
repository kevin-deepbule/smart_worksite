package com.xd.smartworksite.review.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.review.application.ReviewApplicationService;
import com.xd.smartworksite.review.dto.ReviewIssueUpdateRequest;
import com.xd.smartworksite.review.dto.ReviewRecordQueryRequest;
import com.xd.smartworksite.review.dto.ReviewRecordResponse;
import com.xd.smartworksite.review.dto.ReviewRuleResultResponse;
import com.xd.smartworksite.review.dto.ReviewSubmitRequest;
import com.xd.smartworksite.task.dto.TaskStageLogResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/review")
@Validated
public class ReviewController {
    private final ReviewApplicationService reviewApplicationService;

    public ReviewController(ReviewApplicationService reviewApplicationService) {
        this.reviewApplicationService = reviewApplicationService;
    }

    @PostMapping(value = "/records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('review:manage')")
    public ApiResponse<ReviewRecordResponse> submitReview(@Valid ReviewSubmitRequest request) {
        return ApiResponse.success(reviewApplicationService.submitReview(request));
    }

    @GetMapping("/records")
    @PreAuthorize("hasAuthority('review:view')")
    public ApiResponse<PageResult<ReviewRecordResponse>> listRecords(@Valid ReviewRecordQueryRequest request) {
        return ApiResponse.success(reviewApplicationService.queryRecords(request));
    }

    @GetMapping("/records/{recordId}")
    @PreAuthorize("hasAuthority('review:view')")
    public ApiResponse<ReviewRecordResponse> getRecord(@PathVariable Long recordId) {
        return ApiResponse.success(reviewApplicationService.getRecord(recordId));
    }

    @GetMapping("/records/{recordId}/rule-results")
    @PreAuthorize("hasAuthority('review:view')")
    public ApiResponse<List<ReviewRuleResultResponse>> getRuleResults(@PathVariable Long recordId) {
        return ApiResponse.success(reviewApplicationService.getRuleResults(recordId));
    }

    @GetMapping("/records/{recordId}/rule-results/{ruleCode}")
    @PreAuthorize("hasAuthority('review:view')")
    public ApiResponse<ReviewRuleResultResponse> getRuleResult(
            @PathVariable Long recordId,
            @PathVariable String ruleCode) {
        return ApiResponse.success(reviewApplicationService.getRuleResult(recordId, ruleCode));
    }

    @GetMapping("/records/{recordId}/stages")
    @PreAuthorize("hasAuthority('review:view')")
    public ApiResponse<List<TaskStageLogResponse>> getStages(@PathVariable Long recordId) {
        return ApiResponse.success(reviewApplicationService.getStages(recordId));
    }

    @PostMapping("/records/{recordId}/retry")
    @PreAuthorize("hasAuthority('review:manage')")
    public ApiResponse<ReviewRecordResponse> retry(@PathVariable Long recordId) {
        return ApiResponse.success(reviewApplicationService.retry(recordId));
    }

    @PostMapping("/records/{recordId}/cancel")
    @PreAuthorize("hasAuthority('review:manage')")
    public ApiResponse<ReviewRecordResponse> cancel(@PathVariable Long recordId) {
        return ApiResponse.success(reviewApplicationService.cancel(recordId));
    }

    @DeleteMapping("/records/{recordId}")
    @PreAuthorize("hasAuthority('review:manage')")
    public ApiResponse<Void> delete(@PathVariable Long recordId) {
        reviewApplicationService.delete(recordId);
        return ApiResponse.success();
    }

    @PostMapping("/records/{recordId}/archive")
    @PreAuthorize("hasAuthority('review:manage')")
    public ApiResponse<ReviewRecordResponse> archive(@PathVariable Long recordId) {
        return ApiResponse.success(reviewApplicationService.archive(recordId));
    }

    @PutMapping("/records/{recordId}/issues/{issueId}")
    @PreAuthorize("hasAuthority('review:manage')")
    public ApiResponse<ReviewRecordResponse> updateIssue(
            @PathVariable Long recordId,
            @PathVariable String issueId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ReviewIssueUpdateRequest request) {
        return ApiResponse.success(reviewApplicationService.updateIssue(recordId, issueId, request));
    }
}
