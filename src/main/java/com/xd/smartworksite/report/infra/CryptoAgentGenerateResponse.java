package com.xd.smartworksite.report.infra;

import java.util.List;
import java.util.Map;

public class CryptoAgentGenerateResponse {

    private boolean success;
    private String taskId;
    private String reportId;
    private String stage;
    private String conclusion;
    private List<GeneratedFilePayload> generatedFiles;
    private String errorCode;
    private String errorMessage;
    private Map<String, Object> raw;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public List<GeneratedFilePayload> getGeneratedFiles() { return generatedFiles; }
    public void setGeneratedFiles(List<GeneratedFilePayload> generatedFiles) { this.generatedFiles = generatedFiles; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Map<String, Object> getRaw() { return raw; }
    public void setRaw(Map<String, Object> raw) { this.raw = raw; }
}
