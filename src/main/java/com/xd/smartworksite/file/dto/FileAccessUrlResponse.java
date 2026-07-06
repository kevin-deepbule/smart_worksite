package com.xd.smartworksite.file.dto;

import java.time.LocalDateTime;

public class FileAccessUrlResponse {

    private Long fileId;
    private String url;
    private LocalDateTime expiresAt;
    private Boolean previewSupported;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getPreviewSupported() {
        return previewSupported;
    }

    public void setPreviewSupported(Boolean previewSupported) {
        this.previewSupported = previewSupported;
    }
}
