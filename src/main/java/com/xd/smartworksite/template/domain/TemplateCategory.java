package com.xd.smartworksite.template.domain;

import java.util.Locale;

public enum TemplateCategory {
    REVIEW,
    REPORT;

    public static TemplateCategory parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("templateCategory is required");
        }
        return TemplateCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
