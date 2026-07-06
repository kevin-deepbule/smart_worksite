package com.xd.smartworksite.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public class ReportCreateRequest {

    @NotNull
    private Long projectId;

    @Size(max = 128)
    private String reportName;

    @NotBlank
    @Size(max = 64)
    private String reportType;

    private Long templateId;
    private List<Long> referenceFileIds;
    private List<Long> knowledgeBaseIds;
    private List<Long> dataSourceIds;
    private Map<String, Object> generationParams;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public List<Long> getReferenceFileIds() { return referenceFileIds; }
    public void setReferenceFileIds(List<Long> referenceFileIds) { this.referenceFileIds = referenceFileIds; }
    public List<Long> getKnowledgeBaseIds() { return knowledgeBaseIds; }
    public void setKnowledgeBaseIds(List<Long> knowledgeBaseIds) { this.knowledgeBaseIds = knowledgeBaseIds; }
    public List<Long> getDataSourceIds() { return dataSourceIds; }
    public void setDataSourceIds(List<Long> dataSourceIds) { this.dataSourceIds = dataSourceIds; }
    public Map<String, Object> getGenerationParams() { return generationParams; }
    public void setGenerationParams(Map<String, Object> generationParams) { this.generationParams = generationParams; }
}
