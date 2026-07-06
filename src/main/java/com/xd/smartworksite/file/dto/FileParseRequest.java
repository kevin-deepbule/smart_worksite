package com.xd.smartworksite.file.dto;

import jakarta.validation.constraints.NotNull;

public class FileParseRequest {

    @NotNull
    private Long projectId;

    private Boolean force = false;
    private String targetFormat;
    private String language = "zh-CN";

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Boolean getForce() {
        return force;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
