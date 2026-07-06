package com.xd.smartworksite.report.dto;

public class ReportCreateResponse {

    private Long reportId;
    private Long taskId;
    private String status;

    public ReportCreateResponse(Long reportId, Long taskId, String status) {
        this.reportId = reportId;
        this.taskId = taskId;
        this.status = status;
    }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
