package com.xd.smartworksite.report.infra;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReferenceDocumentPayload {

    @JsonProperty("fileId")
    private String fileId;
    @JsonProperty("fileName")
    private String fileName;
    @JsonProperty("content")
    private String content;

    public ReferenceDocumentPayload() {}

    public ReferenceDocumentPayload(String fileId, String fileName, String content) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.content = content;
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
