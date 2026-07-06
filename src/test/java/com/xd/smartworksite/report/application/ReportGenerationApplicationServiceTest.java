package com.xd.smartworksite.report.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.file.infra.StorageAdapter;
import com.xd.smartworksite.file.infra.StorageObject;
import com.xd.smartworksite.project.domain.Project;
import com.xd.smartworksite.project.repository.ProjectRepository;
import com.xd.smartworksite.report.domain.GenerateTask;
import com.xd.smartworksite.report.domain.Report;
import com.xd.smartworksite.report.domain.ReportConfig;
import com.xd.smartworksite.report.domain.ReportVersion;
import com.xd.smartworksite.report.dto.ReportCreateRequest;
import com.xd.smartworksite.report.dto.ReportCreateResponse;
import com.xd.smartworksite.report.infra.CryptoAgentGenerateRequest;
import com.xd.smartworksite.report.infra.CryptoAgentGenerateResponse;
import com.xd.smartworksite.report.infra.CryptoAgentV3ReportClient;
import com.xd.smartworksite.report.infra.CryptoAgentV3Properties;
import com.xd.smartworksite.report.infra.GeneratedFilePayload;
import com.xd.smartworksite.report.repository.ReportRepository;
import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;
import com.xd.smartworksite.template.domain.TemplateCategory;
import com.xd.smartworksite.template.domain.TemplateStatus;
import com.xd.smartworksite.template.repository.TemplateRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

class ReportGenerationApplicationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createReportCallsCryptoAgentAndStoresDownloadRefWithoutDownloadingWord() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateRequest request = createRequest();

        ReportCreateResponse response = service.createReport(request);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(cryptoClient.lastRequest.getReportId()).isEqualTo(String.valueOf(response.getReportId()));
        assertThat(cryptoClient.lastRequest.getTemplateVariables()).isEmpty();
        assertThat(cryptoClient.lastRequest.getReferenceDocuments()).hasSize(1);
        assertThat(cryptoClient.lastRequest.getReferenceDocuments().get(0).getContent()).contains("密测过程信息");
        assertThat(reportRepository.fileObjects).isEmpty();
        assertThat(reportRepository.versions).hasSize(1);
        assertThat(reportRepository.versions.get(0).getWordFileId()).isNull();
        assertThat(reportRepository.reports.get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(reportRepository.reports.get(0).getPreviewUrl()).contains("/report.docx");
    }

    @Test
    void regenerateReportKeepsOriginalGenerationParams() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateResponse first = service.createReport(createRequest());

        service.regenerateReport(first.getReportId());

        assertThat(reportRepository.configs).hasSize(2);
        assertThat(cryptoClient.lastRequest.getReferenceDocuments()).hasSize(1);
        assertThat(cryptoClient.lastRequest.getReferenceDocuments().get(0).getFileName()).isEqualTo("密测过程信息.txt");
        assertThat(cryptoClient.lastRequest.getReferenceDocuments().get(0).getContent()).contains("密测过程信息");
    }


    @Test
    void downloadReportSavesWordOnDemandAndReturnsAccessUrl() throws Exception {
        InMemoryReportRepository reportRepository = new InMemoryReportRepository();
        CapturingCryptoAgentClient cryptoClient = new CapturingCryptoAgentClient(objectMapper, generatedFileUrl());
        ReportGenerationApplicationService service = newService(reportRepository, cryptoClient);
        ReportCreateResponse created = service.createReport(createRequest());

        String downloadUrl = service.createDownloadUrl(created.getReportId(), "WORD");

        assertThat(downloadUrl).startsWith("http://127.0.0.1/reports/project-1/report-1/");
        assertThat(reportRepository.fileObjects).hasSize(1);
        assertThat(reportRepository.versions.get(0).getWordFileId()).isEqualTo(1L);
    }

    private ReportGenerationApplicationService newService(InMemoryReportRepository reportRepository,
                                                          CapturingCryptoAgentClient cryptoClient) {
        return new ReportGenerationApplicationService(
                reportRepository,
                projectRepository(),
                templateRepository(),
                new MemoryStorageAdapter(),
                cryptoClient,
                objectMapper
        );
    }

    private ReportCreateRequest createRequest() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileId", "manual-1");
        document.put("fileName", "密测过程信息.txt");
        document.put("content", "这里是密测过程信息。");
        Map<String, Object> generationParams = new LinkedHashMap<>();
        generationParams.put("referenceDocuments", List.of(document));

        ReportCreateRequest request = new ReportCreateRequest();
        request.setProjectId(1L);
        request.setReportName("密评报告测试");
        request.setReportType("CRYPTO_EVALUATION_REPORT");
        request.setTemplateId(1001L);
        request.setGenerationParams(generationParams);
        return request;
    }

    private ProjectRepository projectRepository() {
        return new ProjectRepository() {
            @Override
            public List<Project> findPage(String keyword) {
                return List.of();
            }

            @Override
            public Optional<Project> findById(Long projectId) {
                Project project = new Project();
                project.setId(projectId);
                return Optional.of(project);
            }
        };
    }

    private TemplateRepository templateRepository() {
        return new TemplateRepository() {
            @Override
            public FileObjectRecord saveFileObject(FileObjectRecord fileObject) {
                return fileObject;
            }

            @Override
            public void updateFileBizId(Long fileId, Long bizId) {
            }

            @Override
            public Template save(Template template) {
                return template;
            }

            @Override
            public Optional<Template> findById(Long templateId) {
                Template template = new Template();
                template.setId(templateId);
                template.setProjectId(1L);
                template.setTemplateCategory(TemplateCategory.REPORT.name());
                template.setStatus(TemplateStatus.ENABLED.name());
                return Optional.of(template);
            }

            @Override
            public List<Template> findPage(Long projectId, String templateCategory, String templateType, String status, String keyword) {
                return List.of();
            }

            @Override
            public void update(Template template) {
            }

            @Override
            public void updateStatus(Long templateId, String status) {
            }

            @Override
            public void delete(Long templateId) {
            }
        };
    }

    private String generatedFileUrl() throws Exception {
        byte[] docxBytes = "generated-docx".getBytes();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/report.docx", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            exchange.sendResponseHeaders(200, docxBytes.length);
            exchange.getResponseBody().write(docxBytes);
            exchange.close();
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/report.docx";
    }

    private static class CapturingCryptoAgentClient extends CryptoAgentV3ReportClient {
        private final String generatedFileUrl;
        private CryptoAgentGenerateRequest lastRequest;

        CapturingCryptoAgentClient(ObjectMapper objectMapper, String generatedFileUrl) {
            super(new CryptoAgentV3Properties(), objectMapper);
            this.generatedFileUrl = generatedFileUrl;
        }

        @Override
        public CryptoAgentGenerateResponse generate(CryptoAgentGenerateRequest requestPayload) {
            this.lastRequest = requestPayload;
            GeneratedFilePayload file = new GeneratedFilePayload();
            file.setFileType("DOCX");
            file.setFileName("密评报告.docx");
            file.setDownloadRef(generatedFileUrl);
            CryptoAgentGenerateResponse response = new CryptoAgentGenerateResponse();
            response.setSuccess(true);
            response.setTaskId(requestPayload.getTaskId());
            response.setReportId(requestPayload.getReportId());
            response.setGeneratedFiles(List.of(file));
            return response;
        }
    }

    private static class MemoryStorageAdapter implements StorageAdapter {
        @Override
        public StorageObject upload(String objectName, InputStream inputStream, long size, String contentType) {
            return new StorageObject(objectName, "test-bucket",contentType, size);
        }

        
        @Override
        public InputStream openObject(String objectName) {
            return InputStream.nullInputStream();
        }

        @Override
        public String createAccessUrl(String objectName, Duration expire) {
            return "http://127.0.0.1/" + objectName;
        }

        @Override
        public void delete(String objectName) {
        }
    }

    private class InMemoryReportRepository implements ReportRepository {
        private long nextConfigId = 1L;
        private long nextReportId = 1L;
        private long nextTaskId = 1L;
        private long nextFileId = 1L;
        private long nextVersionId = 1L;
        private final List<ReportConfig> configs = new ArrayList<>();
        private final List<Report> reports = new ArrayList<>();
        private final List<GenerateTask> tasks = new ArrayList<>();
        private final List<FileObjectRecord> fileObjects = new ArrayList<>();
        private final List<ReportVersion> versions = new ArrayList<>();

        @Override
        public ReportConfig saveConfig(ReportConfig config) {
            config.setId(nextConfigId++);
            configs.add(config);
            return config;
        }

        @Override
        public Report saveReport(Report report) {
            report.setId(nextReportId++);
            reports.add(report);
            return report;
        }

        @Override
        public GenerateTask saveTask(GenerateTask task) {
            task.setId(nextTaskId++);
            tasks.add(task);
            return task;
        }

        @Override
        public void updateReportTask(Long reportId, Long taskId) {
            findReport(reportId).setTaskId(taskId);
        }

        @Override
        public void updateTaskBizId(Long taskId, Long bizId) {
        }

        @Override
        public void updateReportProcessing(Long reportId, String status, int progress, String currentStage) {
            Report report = findReport(reportId);
            report.setStatus(status);
            report.setProgress(progress);
        }

        @Override
        public void updateReportSuccess(Long reportId, Long versionId, String status, int progress, String previewUrl) {
            Report report = findReport(reportId);
            report.setCurrentVersionId(versionId);
            report.setStatus(status);
            report.setProgress(progress);
            report.setPreviewUrl(previewUrl);
        }

        @Override
        public void updateReportFailed(Long reportId, String status, String errorMessage) {
            Report report = findReport(reportId);
            report.setStatus(status);
            report.setErrorMessage(errorMessage);
        }

        @Override
        public void updateTaskStatus(Long taskId, String status, String currentStage, String errorMessage) {
        }

        @Override
        public void updateVersionWordFile(Long versionId, Long wordFileId, String contentHash) {
            versions.stream()
                    .filter(version -> versionId.equals(version.getId()))
                    .findFirst()
                    .orElseThrow()
                    .setWordFileId(wordFileId);
        }

        @Override
        public Optional<FileObjectRecord> findFileObjectById(Long fileId) {
            return fileObjects.stream().filter(file -> fileId.equals(file.getId())).findFirst();
        }

        @Override
        public FileObjectRecord saveFileObject(FileObjectRecord fileObject) {
            fileObject.setId(nextFileId++);
            fileObjects.add(fileObject);
            return fileObject;
        }

        @Override
        public ReportVersion saveVersion(ReportVersion version) {
            version.setId(nextVersionId++);
            versions.add(version);
            return version;
        }

        @Override
        public Optional<ReportConfig> findConfigById(Long configId) {
            return configs.stream().filter(config -> configId.equals(config.getId())).findFirst();
        }

        @Override
        public Optional<Long> findCurrentWordFileId(Long reportId) {
            return versions.stream()
                    .filter(version -> reportId.equals(version.getReportId()))
                    .findFirst()
                    .map(ReportVersion::getWordFileId);
        }

        @Override
        public Optional<Report> findReportById(Long reportId) {
            return reports.stream().filter(report -> reportId.equals(report.getId())).findFirst();
        }

        @Override
        public List<Report> findReportPage(Long projectId, String reportType, String status, String keyword) {
            return reports;
        }

        private Report findReport(Long reportId) {
            return findReportById(reportId).orElseThrow();
        }
    }
}
