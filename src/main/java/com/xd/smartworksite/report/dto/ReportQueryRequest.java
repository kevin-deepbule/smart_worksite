package com.xd.smartworksite.report.dto;

import jakarta.validation.constraints.Min;

public class ReportQueryRequest {

    private Long projectId;
    private String reportType;
    private String status;
    private String keyword;

    @Min(1)
    private int pageNo = 1;

    @Min(1)
    private int pageSize = 20;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public int getPageNo() { return pageNo; }
    public void setPageNo(int pageNo) { this.pageNo = pageNo; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
