package com.xd.smartworksite.file.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.file")
public class FileProperties {

    private long accessUrlExpireSeconds = 600;
    private long maxSizeBytes = 104857600;
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "application/pdf",
            "text/plain",
            "image/png",
            "image/jpeg",
            "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    ));

    public long getAccessUrlExpireSeconds() {
        return accessUrlExpireSeconds;
    }

    public void setAccessUrlExpireSeconds(long accessUrlExpireSeconds) {
        this.accessUrlExpireSeconds = accessUrlExpireSeconds;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }
}
