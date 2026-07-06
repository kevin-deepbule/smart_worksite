package com.xd.smartworksite.template.repository;

import com.xd.smartworksite.template.domain.FileObjectRecord;
import com.xd.smartworksite.template.domain.Template;
import com.xd.smartworksite.template.mapper.TemplateMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisTemplateRepository implements TemplateRepository {

    private final TemplateMapper templateMapper;

    public MyBatisTemplateRepository(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Override
    public FileObjectRecord saveFileObject(FileObjectRecord fileObject) {
        templateMapper.insertFileObject(fileObject);
        return fileObject;
    }

    @Override
    public void updateFileBizId(Long fileId, Long bizId) {
        templateMapper.updateFileBizId(fileId, bizId);
    }

    @Override
    public Template save(Template template) {
        templateMapper.insertTemplate(template);
        return template;
    }

    @Override
    public Optional<Template> findById(Long templateId) {
        return Optional.ofNullable(templateMapper.selectById(templateId));
    }

    @Override
    public List<Template> findPage(Long projectId, String templateCategory, String templateType, String status, String keyword) {
        return templateMapper.selectPage(projectId, templateCategory, templateType, status, keyword);
    }

    @Override
    public void update(Template template) {
        templateMapper.updateTemplate(template);
    }

    @Override
    public void updateStatus(Long templateId, String status) {
        templateMapper.updateStatus(templateId, status);
    }

    @Override
    public void delete(Long templateId) {
        templateMapper.logicalDelete(templateId);
    }
}
