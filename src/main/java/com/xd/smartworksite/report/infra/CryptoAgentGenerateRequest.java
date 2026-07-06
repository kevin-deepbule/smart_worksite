package com.xd.smartworksite.report.infra;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class CryptoAgentGenerateRequest {

    @JsonProperty("taskId")
    private String taskId;
    @JsonProperty("reportId")
    private String reportId;
    @JsonProperty("templateVariables")
    private Map<String, Object> templateVariables = Map.of();
    @JsonProperty("referenceDocuments")
    private List<ReferenceDocumentPayload> referenceDocuments;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public Map<String, Object> getTemplateVariables() { return templateVariables; }
    public void setTemplateVariables(Map<String, Object> templateVariables) { this.templateVariables = templateVariables; }
    public List<ReferenceDocumentPayload> getReferenceDocuments() { return referenceDocuments; }
    public void setReferenceDocuments(List<ReferenceDocumentPayload> referenceDocuments) { this.referenceDocuments = referenceDocuments; }
}
