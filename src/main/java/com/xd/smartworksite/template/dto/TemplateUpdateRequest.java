package com.xd.smartworksite.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TemplateUpdateRequest {

    @NotBlank
    @Size(max = 128)
    private String templateName;

    @NotBlank
    @Size(max = 64)
    private String templateType;

    @Size(max = 128)
    private String scenario;

    @NotBlank
    @Size(max = 32)
    private String versionNo;

    @Size(max = 500)
    private String description;

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
