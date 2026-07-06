package com.xd.smartworksite.template.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.template.application.TemplateApplicationService;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.dto.TemplateQueryRequest;
import com.xd.smartworksite.template.dto.TemplateResponse;
import com.xd.smartworksite.template.dto.TemplateStatusResponse;
import com.xd.smartworksite.template.dto.TemplateUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/templates")
@Validated
public class TemplateController {

    private final TemplateApplicationService templateApplicationService;

    public TemplateController(TemplateApplicationService templateApplicationService) {
        this.templateApplicationService = templateApplicationService;
    }

    @PostMapping
    public ApiResponse<TemplateResponse> uploadTemplate(@RequestParam Long projectId,
                                                        @RequestParam String templateCategory,
                                                        @RequestParam String templateName,
                                                        @RequestParam String templateType,
                                                        @RequestParam(required = false) String scenario,
                                                        @RequestParam(required = false) String versionNo,
                                                        @RequestParam(required = false) String description,
                                                        @RequestParam MultipartFile file) {
        return ApiResponse.success(templateApplicationService.uploadTemplate(
                projectId, templateCategory, templateName, templateType, scenario, versionNo, description, file));
    }

    @GetMapping
    public ApiResponse<PageResult<TemplateResponse>> listTemplates(@Valid TemplateQueryRequest request) {
        return ApiResponse.success(templateApplicationService.queryTemplates(request));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateResponse> getTemplate(@PathVariable Long templateId) {
        return ApiResponse.success(templateApplicationService.getTemplate(templateId));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<TemplateResponse> updateTemplate(@PathVariable Long templateId,
                                                        @Valid @RequestBody TemplateUpdateRequest request) {
        return ApiResponse.success(templateApplicationService.updateTemplate(templateId, request));
    }

    @PostMapping("/{templateId}/enable")
    public ApiResponse<TemplateStatusResponse> enableTemplate(@PathVariable Long templateId) {
        TemplateResponse template = templateApplicationService.enableTemplate(templateId);
        return ApiResponse.success(new TemplateStatusResponse(template.getTemplateId(), template.getStatus()));
    }

    @PostMapping("/{templateId}/disable")
    public ApiResponse<TemplateStatusResponse> disableTemplate(@PathVariable Long templateId) {
        TemplateResponse template = templateApplicationService.disableTemplate(templateId);
        return ApiResponse.success(new TemplateStatusResponse(template.getTemplateId(), template.getStatus()));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long templateId) {
        templateApplicationService.deleteTemplate(templateId);
        return ApiResponse.success();
    }

    @PostMapping("/report")
    public ApiResponse<TemplateResponse> uploadReportTemplate(@RequestParam Long projectId,
                                                              @RequestParam String templateName,
                                                              @RequestParam String templateType,
                                                              @RequestParam(required = false) String scenario,
                                                              @RequestParam(required = false) String versionNo,
                                                              @RequestParam(required = false) String description,
                                                              @RequestParam MultipartFile file) {
        return ApiResponse.success(templateApplicationService.uploadTemplate(
                projectId, TemplateCategory.REPORT.name(), templateName, templateType, scenario, versionNo, description, file));
    }

    @PostMapping("/review")
    public ApiResponse<TemplateResponse> uploadReviewTemplate(@RequestParam Long projectId,
                                                              @RequestParam String templateName,
                                                              @RequestParam String templateType,
                                                              @RequestParam(required = false) String scenario,
                                                              @RequestParam(required = false) String versionNo,
                                                              @RequestParam(required = false) String description,
                                                              @RequestParam MultipartFile file) {
        return ApiResponse.success(templateApplicationService.uploadTemplate(
                projectId, TemplateCategory.REVIEW.name(), templateName, templateType, scenario, versionNo, description, file));
    }
}
