package com.xd.smartworksite.report.repository;

import com.xd.smartworksite.report.domain.GenerateTask;
import com.xd.smartworksite.report.domain.Report;
import com.xd.smartworksite.report.domain.ReportConfig;
import com.xd.smartworksite.report.domain.ReportVersion;
import com.xd.smartworksite.template.domain.FileObjectRecord;

import java.util.List;
import java.util.Optional;

public interface ReportRepository {

    ReportConfig saveConfig(ReportConfig config);

    Report saveReport(Report report);

    GenerateTask saveTask(GenerateTask task);

    void updateReportTask(Long reportId, Long taskId);
    void updateTaskBizId(Long taskId, Long bizId);
    void updateReportProcessing(Long reportId, String status, int progress, String currentStage);
    void updateReportSuccess(Long reportId, Long versionId, String status, int progress, String previewUrl);
    void updateReportFailed(Long reportId, String status, String errorMessage);
    void updateTaskStatus(Long taskId, String status, String currentStage, String errorMessage);
    Optional<FileObjectRecord> findFileObjectById(Long fileId);
    FileObjectRecord saveFileObject(FileObjectRecord fileObject);
    ReportVersion saveVersion(ReportVersion version);
    void updateVersionWordFile(Long versionId, Long wordFileId, String contentHash);
    Optional<ReportConfig> findConfigById(Long configId);
    Optional<Long> findCurrentWordFileId(Long reportId);
    Optional<Report> findReportById(Long reportId);
    List<Report> findReportPage(Long projectId, String reportType, String status, String keyword);
}
