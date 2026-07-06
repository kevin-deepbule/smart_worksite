package com.xd.smartworksite.template.application;

import com.xd.smartworksite.file.infra.StorageAdapter;
import com.xd.smartworksite.file.infra.StorageObject;
import com.xd.smartworksite.project.domain.Project;
import com.xd.smartworksite.project.repository.ProjectRepository;
import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;
import com.xd.smartworksite.template.domain.TemplateStatus;
import com.xd.smartworksite.template.dto.TemplateResponse;
import com.xd.smartworksite.template.repository.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateApplicationServiceTest {

    @Test
    void uploadReportTemplateStoresFileObjectAndTemplateMetadata() {
        InMemoryTemplateRepository templateRepository = new InMemoryTemplateRepository();
        TemplateApplicationService service = new TemplateApplicationService(
                templateRepository,
                projectRepository(),
                new CapturingStorageAdapter()
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report-template.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "template".getBytes()
        );

        TemplateResponse response = service.uploadTemplate(
                1L,
                "REPORT",
                "密评报告模板",
                "CRYPTO_EVALUATION_REPORT",
                "密评报告生成",
                "",
                "第一版模板",
                file
        );

        assertThat(response.getTemplateId()).isEqualTo(1L);
        assertThat(response.getTemplateCategory()).isEqualTo("REPORT");
        assertThat(response.getTemplateType()).isEqualTo("CRYPTO_EVALUATION_REPORT");
        assertThat(response.getVersionNo()).isEqualTo("v1");
        assertThat(response.getStatus()).isEqualTo(TemplateStatus.ENABLED.name());
        assertThat(templateRepository.fileObjects).hasSize(1);
        assertThat(templateRepository.fileObjects.get(0).getBizType()).isEqualTo("REPORT_TEMPLATE");
        assertThat(templateRepository.fileObjects.get(0).getBizId()).isEqualTo(1L);
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

    private static class CapturingStorageAdapter implements StorageAdapter {
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

    private static class InMemoryTemplateRepository implements TemplateRepository {
        private long nextFileId = 1L;
        private long nextTemplateId = 1L;
        private final List<FileObjectRecord> fileObjects = new ArrayList<>();
        private final List<Template> templates = new ArrayList<>();

        @Override
        public FileObjectRecord saveFileObject(FileObjectRecord fileObject) {
            fileObject.setId(nextFileId++);
            fileObjects.add(fileObject);
            return fileObject;
        }

        @Override
        public void updateFileBizId(Long fileId, Long bizId) {
            fileObjects.stream()
                    .filter(file -> fileId.equals(file.getId()))
                    .findFirst()
                    .orElseThrow()
                    .setBizId(bizId);
        }

        @Override
        public Template save(Template template) {
            template.setId(nextTemplateId++);
            templates.add(template);
            return template;
        }

        @Override
        public Optional<Template> findById(Long templateId) {
            return templates.stream().filter(template -> templateId.equals(template.getId())).findFirst();
        }

        @Override
        public List<Template> findPage(Long projectId, String templateCategory, String templateType, String status, String keyword) {
            return templates;
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
    }
}
