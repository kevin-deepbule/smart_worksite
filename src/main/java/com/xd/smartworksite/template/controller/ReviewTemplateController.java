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
@RequestMapping("/api/review/templates")
@Validated
public class ReviewTemplateController {

    private final TemplateApplicationService templateApplicationService;

    public ReviewTemplateController(TemplateApplicationService templateApplicationService) {
        this.templateApplicationService = templateApplicationService;
    }

    @PostMapping
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

    @GetMapping
    public ApiResponse<List<TemplateResponse>> listReviewTemplates(TemplateQueryRequest request) {
        request.setTemplateCategory(TemplateCategory.REVIEW.name());
        return ApiResponse.success(templateApplicationService.listTemplates(request));
    }
}
