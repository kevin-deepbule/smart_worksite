package com.xd.smartworksite.template.repository;

import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository {

    FileObjectRecord saveFileObject(FileObjectRecord fileObject);

    void updateFileBizId(Long fileId, Long bizId);

    Template save(Template template);

    Optional<Template> findById(Long templateId);

    List<Template> findPage(Long projectId, String templateCategory, String templateType, String status, String keyword);

    void update(Template template);

    void updateStatus(Long templateId, String status);

    void delete(Long templateId);
}
