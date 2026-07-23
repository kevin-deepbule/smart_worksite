package com.xd.smartworksite.template.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.security.SecurityUtils;
import com.xd.smartworksite.file.application.FileObjectApplicationService;
import com.xd.smartworksite.file.application.FileObjectContent;
import com.xd.smartworksite.project.application.ProjectAccessApplicationService;
import com.xd.smartworksite.template.domain.Template;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.domain.TemplateVariableDescription;
import com.xd.smartworksite.template.dto.TemplateVariableDescriptionItemRequest;
import com.xd.smartworksite.template.dto.TemplateVariableDescriptionResponse;
import com.xd.smartworksite.template.dto.TemplateVariableDescriptionUpsertRequest;
import com.xd.smartworksite.template.infra.TemplateFileSupport;
import com.xd.smartworksite.template.infra.TemplateVariableScanner;
import com.xd.smartworksite.template.repository.TemplateRepository;
import com.xd.smartworksite.template.repository.TemplateVariableDescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TemplateVariableApplicationService {

    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("^var_[a-z0-9_]+$");

    private final TemplateRepository templateRepository;
    private final TemplateVariableDescriptionRepository descriptionRepository;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final FileObjectApplicationService fileObjectApplicationService;
    private final TemplateVariableScanner variableScanner;

    public TemplateVariableApplicationService(TemplateRepository templateRepository,
                                              TemplateVariableDescriptionRepository descriptionRepository,
                                              ProjectAccessApplicationService projectAccessApplicationService,
                                              FileObjectApplicationService fileObjectApplicationService,
                                              TemplateVariableScanner variableScanner) {
        this.templateRepository = templateRepository;
        this.descriptionRepository = descriptionRepository;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.fileObjectApplicationService = fileObjectApplicationService;
        this.variableScanner = variableScanner;
    }

    public List<String> listVariables(Long templateId) {
        Template template = requireTemplate(templateId, false);
        return new ArrayList<>(readVariableDefinitions(template, openTemplateFile(template)).keySet());
    }

    public List<String> listReportVariables(Long templateId) {
        Template template = requireTemplate(templateId, false);
        if (!TemplateCategory.REPORT.name().equals(template.getTemplateCategory())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "template is not a report template");
        }
        return new ArrayList<>(readVariableDefinitions(template, openTemplateFile(template)).keySet());
    }

    public List<TemplateVariableDescriptionResponse> listDescriptions(Long templateId) {
        Template template = requireTemplate(templateId, false);
        FileObjectContent file = openTemplateFile(template);
        Map<String, String> parsedDefinitions = readVariableDefinitions(template, file);
        List<String> parsedVariables = new ArrayList<>(parsedDefinitions.keySet());
        Map<String, TemplateVariableDescription> persisted = new HashMap<>();
        for (TemplateVariableDescription record : descriptionRepository.findActiveByTemplateAndFile(
                template.getId(), file.getFileId())) {
            persisted.put(record.getVariableName(), record);
        }
        List<TemplateVariableDescriptionResponse> response = new ArrayList<>();
        for (String variableName : parsedVariables) {
            TemplateVariableDescription record = persisted.get(variableName);
            response.add(new TemplateVariableDescriptionResponse(
                    variableName,
                    record == null || record.getDescription() == null
                            ? parsedDefinitions.getOrDefault(variableName, "")
                            : record.getDescription()
            ));
        }
        return response;
    }

    @Transactional
    public List<TemplateVariableDescriptionResponse> upsertDescriptions(
            Long templateId,
            TemplateVariableDescriptionUpsertRequest request) {
        if (request == null || request.getVariables() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "variables不能为空");
        }
        if (request.getVariables().size() > 1000) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "variables不能超过1000项");
        }
        Template template = requireTemplate(templateId, true);
        FileObjectContent file = openTemplateFile(template);
        List<String> parsedVariables = new ArrayList<>(readVariableDefinitions(template, file).keySet());
        Map<String, String> requestDescriptions = normalizeDescriptions(request.getVariables());
        validateVariableSet(parsedVariables, requestDescriptions.keySet());

        Long operatorId = SecurityUtils.getCurrentUserId();
        for (String variableName : parsedVariables) {
            String description = requestDescriptions.get(variableName);
            TemplateVariableDescription record = descriptionRepository
                    .findByKey(template.getId(), file.getFileId(), variableName)
                    .orElseGet(TemplateVariableDescription::new);
            boolean insert = record.getId() == null;
            record.setProjectId(template.getProjectId());
            record.setTemplateId(template.getId());
            record.setFileId(file.getFileId());
            record.setVariableName(variableName);
            record.setDescription(description);
            record.setUpdatedBy(operatorId);
            if (insert) {
                record.setCreatedBy(operatorId);
                int inserted = descriptionRepository.insert(record);
                if (inserted <= 0 || record.getId() == null) {
                    throw new BusinessException(ErrorCode.CONFLICT, "模板变量描述新增失败");
                }
            } else {
                int updated = descriptionRepository.updateAndReactivate(record);
                if (updated <= 0) {
                    throw new BusinessException(ErrorCode.CONFLICT, "模板变量描述更新失败");
                }
            }
        }

        Map<String, TemplateVariableDescription> persisted = new HashMap<>();
        for (TemplateVariableDescription record : descriptionRepository.findActiveByTemplateAndFile(
                template.getId(), file.getFileId())) {
            persisted.put(record.getVariableName(), record);
        }
        List<TemplateVariableDescriptionResponse> response = new ArrayList<>();
        for (String variableName : parsedVariables) {
            TemplateVariableDescription record = persisted.get(variableName);
            if (record == null || !requestDescriptions.get(variableName).equals(record.getDescription())) {
                throw new BusinessException(ErrorCode.CONFLICT, "模板变量描述写入后无法读回");
            }
            response.add(new TemplateVariableDescriptionResponse(variableName, record.getDescription()));
        }
        return response;
    }

    private Template requireTemplate(Long templateId, boolean writableManage) {
        if (templateId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板ID不能为空");
        }
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"));
        if (writableManage) {
            projectAccessApplicationService.requireProjectWritableManage(template.getProjectId());
        } else {
            projectAccessApplicationService.requireProjectAccess(template.getProjectId());
        }
        return template;
    }

    private FileObjectContent openTemplateFile(Template template) {
        return fileObjectApplicationService.openFileContent(
                template.getFileId(), template.getProjectId(), template.getId());
    }

    private Map<String, String> readVariableDefinitions(Template template, FileObjectContent file) {
        if (!TemplateFileSupport.isSupported(file.getFileName())) {
            closeQuietly(file);
            String format = TemplateFileSupport.isPdf(file.getFileName()) ? "PDF" : TemplateFileSupport.extension(file.getFileName());
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported template variable format: " + format);
        }
        try (InputStream inputStream = file.getInputStream()) {
            if (TemplateCategory.REVIEW.name().equals(template.getTemplateCategory())) {
                return variableScanner.scanReviewDescriptions(file.getFileName(), inputStream);
            }
            Map<String, String> definitions = new LinkedHashMap<>();
            for (String variableName : variableScanner.scan(file.getFileName(), inputStream)) {
                definitions.put(variableName, "");
            }
            return definitions;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, ex.getMessage());
        } catch (IOException | RuntimeException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板文件损坏或无法解析");
        }
    }

    private void closeQuietly(FileObjectContent file) {
        try {
            file.getInputStream().close();
        } catch (IOException ignored) {
        }
    }

    private Map<String, String> normalizeDescriptions(List<TemplateVariableDescriptionItemRequest> items) {
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (TemplateVariableDescriptionItemRequest item : items) {
            if (item == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "变量描述项不能为空");
            }
            String variableName = normalize(item.getVariableName());
            String description = normalize(item.getDescription());
            if (variableName == null || variableName.length() > 128 || !VARIABLE_NAME_PATTERN.matcher(variableName).matches()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "非法模板变量名");
            }
            if (description == null || description.length() > 2000) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "变量描述不能为空且不能超过2000个字符");
            }
            if (descriptions.putIfAbsent(variableName, description) != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "模板变量重复: " + variableName);
            }
        }
        return descriptions;
    }

    private void validateVariableSet(List<String> parsedVariables, Set<String> requestedVariables) {
        Set<String> parsedSet = new LinkedHashSet<>(parsedVariables);
        Set<String> missing = new LinkedHashSet<>(parsedSet);
        missing.removeAll(requestedVariables);
        Set<String> unknown = new LinkedHashSet<>(requestedVariables);
        unknown.removeAll(parsedSet);
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "变量描述缺少模板变量: " + String.join(", ", missing));
        }
        if (!unknown.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "变量描述包含未知模板变量: " + String.join(", ", unknown));
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
