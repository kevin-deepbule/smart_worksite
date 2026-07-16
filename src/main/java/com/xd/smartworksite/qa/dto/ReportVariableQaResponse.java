package com.xd.smartworksite.qa.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportVariableQaResponse {
    private String answer;
    private String providerTraceId;
    private List<Map<String, Object>> references = new ArrayList<>();

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getProviderTraceId() { return providerTraceId; }
    public void setProviderTraceId(String providerTraceId) { this.providerTraceId = providerTraceId; }
    public List<Map<String, Object>> getReferences() { return references; }
    public void setReferences(List<Map<String, Object>> references) { this.references = references; }
}
