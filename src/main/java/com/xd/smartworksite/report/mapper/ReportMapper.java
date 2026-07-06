package com.xd.smartworksite.report.mapper;

import com.xd.smartworksite.report.domain.GenerateTask;
import com.xd.smartworksite.report.domain.Report;
import com.xd.smartworksite.report.domain.ReportConfig;
import com.xd.smartworksite.report.domain.ReportVersion;
import com.xd.smartworksite.template.domain.FileObjectRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReportMapper {

    void insertReportConfig(ReportConfig config);

    void insertReport(Report report);

    void insertGenerateTask(GenerateTask task);

    int updateReportTask(@Param("reportId") Long reportId, @Param("taskId") Long taskId);

    int updateTaskBizId(@Param("taskId") Long taskId, @Param("bizId") Long bizId);

    int updateReportProcessing(@Param("reportId") Long reportId,
                               @Param("status") String status,
                               @Param("progress") int progress,
                               @Param("currentStage") String currentStage);

    int updateReportSuccess(@Param("reportId") Long reportId,
                            @Param("versionId") Long versionId,
                            @Param("status") String status,
                            @Param("progress") int progress,
                            @Param("previewUrl") String previewUrl);

    int updateReportFailed(@Param("reportId") Long reportId,
                           @Param("status") String status,
                           @Param("errorMessage") String errorMessage);

    int updateTaskStatus(@Param("taskId") Long taskId,
                         @Param("status") String status,
                         @Param("currentStage") String currentStage,
                         @Param("errorMessage") String errorMessage);

    FileObjectRecord selectFileObjectById(@Param("fileId") Long fileId);

    void insertFileObject(FileObjectRecord fileObject);

    void insertReportVersion(ReportVersion version);

    int updateReportVersionWordFile(@Param("versionId") Long versionId,
                                    @Param("wordFileId") Long wordFileId,
                                    @Param("contentHash") String contentHash);

    Report selectReportById(@Param("reportId") Long reportId);

    ReportConfig selectConfigById(@Param("configId") Long configId);

    Long selectCurrentWordFileId(@Param("reportId") Long reportId);

    List<Report> selectReportPage(@Param("projectId") Long projectId,
                                  @Param("reportType") String reportType,
                                  @Param("status") String status,
                                  @Param("keyword") String keyword);
}
