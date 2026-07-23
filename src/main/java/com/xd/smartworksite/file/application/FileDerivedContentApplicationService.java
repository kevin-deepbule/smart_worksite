package com.xd.smartworksite.file.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.file.infra.StorageAdapter;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class FileDerivedContentApplicationService {
    private final StorageAdapter storageAdapter;
    private final ProjectAccessApplicationService projectAccessApplicationService;

    public FileDerivedContentApplicationService(StorageAdapter storageAdapter,
                                                ProjectAccessApplicationService projectAccessApplicationService) {
        this.storageAdapter = storageAdapter;
        this.projectAccessApplicationService = projectAccessApplicationService;
    }

    public String storeMarkdownForSystem(Long projectId, String scope, Long ownerId,
                                         String itemCode, String content) {
        if (projectId == null || ownerId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "projectId and ownerId are required");
        }
        projectAccessApplicationService.requireProjectWritableForSystem(projectId);
        String safeScope = normalizePathPart(scope, "scope");
        String safeItemCode = normalizePathPart(itemCode, "itemCode");
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "derived Markdown content is empty");
        }
        String objectName = "projects/%d/DERIVED/%s/%d/%s.md".formatted(
                projectId, safeScope.toUpperCase(Locale.ROOT), ownerId, safeItemCode);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try {
            storageAdapter.upload(objectName, new ByteArrayInputStream(bytes), bytes.length,
                    "text/markdown; charset=utf-8");
            return objectName;
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "store derived Markdown content failed");
        }
    }

    private String normalizePathPart(String value, String fieldName) {
        if (value == null || value.isBlank() || !value.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " is invalid");
        }
        return value;
    }
}
