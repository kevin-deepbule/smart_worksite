package com.xd.smartworksite.file.domain;

import java.util.Locale;

public enum FileBizType {
    DOCUMENT,
    IMAGE,
    TEMPLATE,
    REPORT,
    OCR,
    OTHER;

    public static FileBizType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("file business type is required");
        }
        return FileBizType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
