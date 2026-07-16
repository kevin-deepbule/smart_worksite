package com.xd.smartworksite.report.controller;

import com.xd.smartworksite.common.result.ApiResponse;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.report.application.ReportGenerationApplicationService;
import com.xd.smartworksite.report.dto.ReportCreateRequest;
import com.xd.smartworksite.report.dto.ReportCreateResponse;
import com.xd.smartworksite.report.dto.ReportQueryRequest;
import com.xd.smartworksite.report.dto.ReportResponse;
import com.xd.smartworksite.report.dto.ReportVariableResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Validated
public class ReportController {

    private final ReportGenerationApplicationService reportGenerationApplicationService;

    public ReportController(ReportGenerationApplicationService reportGenerationApplicationService) {
        this.reportGenerationApplicationService = reportGenerationApplicationService;
    }

    @PostMapping
    public ApiResponse<ReportCreateResponse> createReport(@Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.success(reportGenerationApplicationService.createReport(request));
    }

    @GetMapping
    public ApiResponse<PageResult<ReportResponse>> listReports(@Valid ReportQueryRequest request) {
        return ApiResponse.success(reportGenerationApplicationService.queryReports(request));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<ReportResponse> getReport(@PathVariable Long reportId) {
        return ApiResponse.success(reportGenerationApplicationService.getReport(reportId));
    }

    @GetMapping("/{reportId}/variables")
    public ApiResponse<List<ReportVariableResponse>> getReportVariables(@PathVariable Long reportId) {
        return ApiResponse.success(reportGenerationApplicationService.getReportVariables(reportId));
    }

    @PostMapping("/{reportId}/regenerate")
    public ApiResponse<ReportCreateResponse> regenerateReport(@PathVariable Long reportId) {
        return ApiResponse.success(reportGenerationApplicationService.regenerateReport(reportId));
    }

    @GetMapping("/{reportId}/download")
    public ApiResponse<String> downloadReport(@PathVariable Long reportId,
                                              @RequestParam(defaultValue = "WORD") String format) {
        return ApiResponse.success(reportGenerationApplicationService.createDownloadUrl(reportId, format));
    }
}
