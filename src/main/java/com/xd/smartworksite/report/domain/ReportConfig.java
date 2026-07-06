package com.xd.smartworksite.report.domain;

public class ReportConfig {

    private Long id;
    private Long projectId;
    private String configName;
    private String reportType;
    private Long templateId;
    private String referenceFileIds;
    private String knowledgeBaseIds;
    private String dataSourceIds;
    private String generationParams;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getReferenceFileIds() { return referenceFileIds; }
    public void setReferenceFileIds(String referenceFileIds) { this.referenceFileIds = referenceFileIds; }
    public String getKnowledgeBaseIds() { return knowledgeBaseIds; }
    public void setKnowledgeBaseIds(String knowledgeBaseIds) { this.knowledgeBaseIds = knowledgeBaseIds; }
    public String getDataSourceIds() { return dataSourceIds; }
    public void setDataSourceIds(String dataSourceIds) { this.dataSourceIds = dataSourceIds; }
    public String getGenerationParams() { return generationParams; }
    public void setGenerationParams(String generationParams) { this.generationParams = generationParams; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
