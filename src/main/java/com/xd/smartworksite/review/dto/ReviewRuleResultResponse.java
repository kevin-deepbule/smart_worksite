package com.xd.smartworksite.review.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReviewRuleResultResponse {
    private Long ruleResultId;
    private String ruleCode;
    private String ruleDescription;
    private Integer ruleOrder;
    private String executionStatus;
    private String complianceStatus;
    private String reason;
    private String suggestion;
    private List<Map<String, Object>> evidence = new ArrayList<>();
    private BigDecimal confidence;
    private String modelTraceId;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public Long getRuleResultId() { return ruleResultId; }
    public void setRuleResultId(Long ruleResultId) { this.ruleResultId = ruleResultId; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getRuleDescription() { return ruleDescription; }
    public void setRuleDescription(String ruleDescription) { this.ruleDescription = ruleDescription; }
    public Integer getRuleOrder() { return ruleOrder; }
    public void setRuleOrder(Integer ruleOrder) { this.ruleOrder = ruleOrder; }
    public String getExecutionStatus() { return executionStatus; }
    public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }
    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public List<Map<String, Object>> getEvidence() { return evidence; }
    public void setEvidence(List<Map<String, Object>> evidence) { this.evidence = evidence; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getModelTraceId() { return modelTraceId; }
    public void setModelTraceId(String modelTraceId) { this.modelTraceId = modelTraceId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
