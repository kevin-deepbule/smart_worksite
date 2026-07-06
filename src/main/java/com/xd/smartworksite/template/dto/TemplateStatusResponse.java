package com.xd.smartworksite.template.dto;

public class TemplateStatusResponse {

    private Long templateId;
    private String status;

    public TemplateStatusResponse(Long templateId, String status) {
        this.templateId = templateId;
        this.status = status;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
