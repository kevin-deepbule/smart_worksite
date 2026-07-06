package com.xd.smartworksite.report.domain;

public class ReportVersion {

    private Long id;
    private Long projectId;
    private Long reportId;
    private int versionNo;
    private Long wordFileId;
    private Long pdfFileId;
    private String sourceSnapshot;
    private String engineResponse;
    private String contentHash;
    private String status;
    private String errorMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public int getVersionNo() { return versionNo; }
    public void setVersionNo(int versionNo) { this.versionNo = versionNo; }
    public Long getWordFileId() { return wordFileId; }
    public void setWordFileId(Long wordFileId) { this.wordFileId = wordFileId; }
    public Long getPdfFileId() { return pdfFileId; }
    public void setPdfFileId(Long pdfFileId) { this.pdfFileId = pdfFileId; }
    public String getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(String sourceSnapshot) { this.sourceSnapshot = sourceSnapshot; }
    public String getEngineResponse() { return engineResponse; }
    public void setEngineResponse(String engineResponse) { this.engineResponse = engineResponse; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
