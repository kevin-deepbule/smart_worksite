package com.xd.smartworksite.report.repository;

import com.xd.smartworksite.report.domain.GenerateTask;
import com.xd.smartworksite.report.domain.Report;
import com.xd.smartworksite.report.domain.ReportConfig;
import com.xd.smartworksite.report.domain.ReportVersion;
import com.xd.smartworksite.report.mapper.ReportMapper;
import com.xd.smartworksite.template.domain.FileObjectRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisReportRepository implements ReportRepository {

    private final ReportMapper reportMapper;

    public MyBatisReportRepository(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    @Override
    public ReportConfig saveConfig(ReportConfig config) {
        reportMapper.insertReportConfig(config);
        return config;
    }

    @Override
    public Report saveReport(Report report) {
        reportMapper.insertReport(report);
        return report;
    }

    @Override
    public GenerateTask saveTask(GenerateTask task) {
        reportMapper.insertGenerateTask(task);
        return task;
    }

    @Override
    public void updateReportTask(Long reportId, Long taskId) {
        reportMapper.updateReportTask(reportId, taskId);
    }

    @Override
    public void updateTaskBizId(Long taskId, Long bizId) {
        reportMapper.updateTaskBizId(taskId, bizId);
    }

    @Override
    public void updateReportProcessing(Long reportId, String status, int progress, String currentStage) {
        reportMapper.updateReportProcessing(reportId, status, progress, currentStage);
    }

    @Override
    public void updateReportSuccess(Long reportId, Long versionId, String status, int progress, String previewUrl) {
        reportMapper.updateReportSuccess(reportId, versionId, status, progress, previewUrl);
    }

    @Override
    public void updateReportFailed(Long reportId, String status, String errorMessage) {
        reportMapper.updateReportFailed(reportId, status, errorMessage);
    }

    @Override
    public void updateTaskStatus(Long taskId, String status, String currentStage, String errorMessage) {
        reportMapper.updateTaskStatus(taskId, status, currentStage, errorMessage);
    }

    @Override
    public Optional<FileObjectRecord> findFileObjectById(Long fileId) {
        return Optional.ofNullable(reportMapper.selectFileObjectById(fileId));
    }

    @Override
    public FileObjectRecord saveFileObject(FileObjectRecord fileObject) {
        reportMapper.insertFileObject(fileObject);
        return fileObject;
    }

    @Override
    public ReportVersion saveVersion(ReportVersion version) {
        reportMapper.insertReportVersion(version);
        return version;
    }

    @Override
    public void updateVersionWordFile(Long versionId, Long wordFileId, String contentHash) {
        reportMapper.updateReportVersionWordFile(versionId, wordFileId, contentHash);
    }

    @Override
    public Optional<ReportConfig> findConfigById(Long configId) {
        return Optional.ofNullable(reportMapper.selectConfigById(configId));
    }

    @Override
    public Optional<Long> findCurrentWordFileId(Long reportId) {
        return Optional.ofNullable(reportMapper.selectCurrentWordFileId(reportId));
    }

    @Override
    public Optional<Report> findReportById(Long reportId) {
        return Optional.ofNullable(reportMapper.selectReportById(reportId));
    }

    @Override
    public List<Report> findReportPage(Long projectId, String reportType, String status, String keyword) {
        return reportMapper.selectReportPage(projectId, reportType, status, keyword);
    }
}
