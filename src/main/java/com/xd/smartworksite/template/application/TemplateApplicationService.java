package com.xd.smartworksite.template.application;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.file.infra.StorageAdapter;
import com.xd.smartworksite.file.infra.StorageObject;
import com.xd.smartworksite.project.repository.ProjectRepository;
import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.domain.TemplateStatus;
import com.xd.smartworksite.template.dto.TemplateQueryRequest;
import com.xd.smartworksite.template.dto.TemplateResponse;
import com.xd.smartworksite.template.dto.TemplateUpdateRequest;
import com.xd.smartworksite.template.repository.TemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TemplateApplicationService {

    private static final String FILE_STATUS_ACTIVE = "ACTIVE";

    private final TemplateRepository templateRepository;
    private final ProjectRepository projectRepository;
    private final StorageAdapter storageAdapter;

    public TemplateApplicationService(TemplateRepository templateRepository,
                                      ProjectRepository projectRepository,
                                      StorageAdapter storageAdapter) {
        this.templateRepository = templateRepository;
        this.projectRepository = projectRepository;
        this.storageAdapter = storageAdapter;
    }

    @Transactional
    public TemplateResponse uploadTemplate(Long projectId,
                                           String templateCategory,
                                           String templateName,
                                           String templateType,
                                           String scenario,
                                           String versionNo,
                                           String description,
                                           MultipartFile file) {
        requireProject(projectId);
        TemplateCategory category = parseCategory(templateCategory);
        requireText(templateName, "模板名称不能为空");
        requireText(templateType, "模板类型不能为空");
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板文件不能为空");
        }

        String normalizedVersion = normalizeVersion(versionNo);
        String originalFilename = normalizeFileName(file.getOriginalFilename());
        String objectName = buildObjectName(projectId, category.name(), originalFilename);
        StorageObject storageObject;
        try {
            storageObject = storageAdapter.upload(objectName, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取模板文件失败");
        }

        FileObjectRecord fileObject = new FileObjectRecord();
        fileObject.setProjectId(projectId);
        fileObject.setBizType(category == TemplateCategory.REPORT ? "REPORT_TEMPLATE" : "REVIEW_TEMPLATE");
        fileObject.setFileName(originalFilename);
        fileObject.setObjectName(storageObject.getObjectName());
        fileObject.setContentType(storageObject.getContentType());
        fileObject.setFileSize(storageObject.getSize());
        fileObject.setStatus(FILE_STATUS_ACTIVE);
        fileObject.setMetadata("{}");
        templateRepository.saveFileObject(fileObject);

        Template template = new Template();
        template.setProjectId(projectId);
        template.setTemplateName(templateName.trim());
        template.setTemplateCategory(category.name());
        template.setTemplateType(templateType.trim());
        template.setScenario(trimToNull(scenario));
        template.setVersionNo(normalizedVersion);
        template.setFileId(fileObject.getId());
        template.setStatus(TemplateStatus.ENABLED.name());
        template.setDescription(trimToNull(description));
        template.setMetadata("{}");
        templateRepository.save(template);
        templateRepository.updateFileBizId(fileObject.getId(), template.getId());

        return toResponse(template);
    }

    public PageResult<TemplateResponse> queryTemplates(TemplateQueryRequest request) {
        String category = normalizeOptionalEnum(request.getTemplateCategory());
        String status = normalizeOptionalEnum(request.getStatus());
        Page<Template> page = PageHelper.startPage(request.getPageNo(), request.getPageSize())
                .doSelectPage(() -> templateRepository.findPage(
                        request.getProjectId(),
                        category,
                        trimToNull(request.getTemplateType()),
                        status,
                        trimToNull(request.getKeyword())
                ));
        List<TemplateResponse> records = page.getResult().stream().map(this::toResponse).toList();
        return new PageResult<>(request.getPageNo(), request.getPageSize(), page.getTotal(), records);
    }

    public List<TemplateResponse> listTemplates(TemplateQueryRequest request) {
        String category = normalizeOptionalEnum(request.getTemplateCategory());
        String status = normalizeOptionalEnum(request.getStatus());
        return templateRepository.findPage(
                        request.getProjectId(),
                        category,
                        trimToNull(request.getTemplateType()),
                        status,
                        trimToNull(request.getKeyword())
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TemplateResponse getTemplate(Long templateId) {
        return toResponse(requireTemplate(templateId));
    }

    @Transactional
    public TemplateResponse updateTemplate(Long templateId, TemplateUpdateRequest request) {
        Template template = requireTemplate(templateId);
        template.setTemplateName(request.getTemplateName().trim());
        template.setTemplateType(request.getTemplateType().trim());
        template.setScenario(trimToNull(request.getScenario()));
        template.setVersionNo(normalizeVersion(request.getVersionNo()));
        template.setDescription(trimToNull(request.getDescription()));
        templateRepository.update(template);
        return toResponse(requireTemplate(templateId));
    }

    @Transactional
    public TemplateResponse enableTemplate(Long templateId) {
        requireTemplate(templateId);
        templateRepository.updateStatus(templateId, TemplateStatus.ENABLED.name());
        return toResponse(requireTemplate(templateId));
    }

    @Transactional
    public TemplateResponse disableTemplate(Long templateId) {
        requireTemplate(templateId);
        templateRepository.updateStatus(templateId, TemplateStatus.DISABLED.name());
        return toResponse(requireTemplate(templateId));
    }

    @Transactional
    public void deleteTemplate(Long templateId) {
        requireTemplate(templateId);
        templateRepository.delete(templateId);
    }

    private void requireProject(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "项目ID不能为空");
        }
        projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "项目不存在"));
    }

    private Template requireTemplate(Long templateId) {
        if (templateId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板ID不能为空");
        }
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"));
    }

    private TemplateCategory parseCategory(String templateCategory) {
        try {
            return TemplateCategory.parse(templateCategory);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板分类必须是 REVIEW 或 REPORT");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private String normalizeVersion(String versionNo) {
        if (versionNo == null || versionNo.isBlank()) {
            return "v1";
        }
        return versionNo.trim();
    }

    private String normalizeFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "template.bin";
        }
        return filename.replace("\\", "/").substring(filename.replace("\\", "/").lastIndexOf('/') + 1);
    }

    private String buildObjectName(Long projectId, String category, String filename) {
        String suffix = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            suffix = filename.substring(dotIndex).toLowerCase(Locale.ROOT);
        }
        return "templates/project-" + projectId + "/" + category.toLowerCase(Locale.ROOT) + "/"
                + LocalDate.now() + "/" + UUID.randomUUID() + suffix;
    }

    private String normalizeOptionalEnum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private TemplateResponse toResponse(Template template) {
        TemplateResponse response = new TemplateResponse();
        response.setId(template.getId());
        response.setTemplateId(template.getId());
        response.setProjectId(template.getProjectId());
        response.setTaskId(0L);
        response.setFileId(template.getFileId());
        response.setTemplateCategory(template.getTemplateCategory());
        response.setTemplateName(template.getTemplateName());
        response.setTemplateType(template.getTemplateType());
        response.setScenario(template.getScenario());
        response.setVersionNo(template.getVersionNo());
        response.setStatus(template.getStatus());
        response.setDescription(template.getDescription());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }
}
