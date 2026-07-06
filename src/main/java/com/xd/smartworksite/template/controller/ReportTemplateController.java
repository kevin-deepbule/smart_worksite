package com.xd.smartworksite.template.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.template.application.TemplateApplicationService;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.dto.TemplateQueryRequest;
import com.xd.smartworksite.template.dto.TemplateResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/report/templates")
@Validated
public class ReportTemplateController {

    private final TemplateApplicationService templateApplicationService;

    public ReportTemplateController(TemplateApplicationService templateApplicationService) {
        this.templateApplicationService = templateApplicationService;
    }

    @PostMapping
    public ApiResponse<TemplateResponse> uploadReportTemplate(@RequestParam Long projectId,
                                                              @RequestParam(required = false) String templateName,
                                                              @RequestParam(required = false) String templateType,
                                                              @RequestParam(required = false) String scenario,
                                                              @RequestParam(required = false) String versionNo,
                                                              @RequestParam(required = false) String description,
                                                              @RequestParam MultipartFile file) {
        String resolvedName = templateName == null || templateName.isBlank() ? file.getOriginalFilename() : templateName;
        String resolvedType = templateType == null || templateType.isBlank() ? "CRYPTO_EVALUATION_REPORT" : templateType;
        return ApiResponse.success(templateApplicationService.uploadTemplate(
                projectId, TemplateCategory.REPORT.name(), resolvedName, resolvedType, scenario, versionNo, description, file));
    }

    @GetMapping
    public ApiResponse<List<TemplateResponse>> listReportTemplates(TemplateQueryRequest request) {
        request.setTemplateCategory(TemplateCategory.REPORT.name());
        return ApiResponse.success(templateApplicationService.listTemplates(request));
    }
}
