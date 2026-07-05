package com.xd.smartworksite.file.repository;

import com.xd.smartworksite.file.domain.FileObject;
import com.xd.smartworksite.file.dto.FileQueryRequest;
import com.xd.smartworksite.file.mapper.FileObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisFileObjectRepository implements FileObjectRepository {

    private final FileObjectMapper fileObjectMapper;

    public MyBatisFileObjectRepository(FileObjectMapper fileObjectMapper) {
        this.fileObjectMapper = fileObjectMapper;
    }

    @Override
    public void insert(FileObject fileObject) {
        fileObjectMapper.insert(fileObject);
    }

    @Override
    public List<FileObject> findPage(FileQueryRequest request) {
        return fileObjectMapper.selectPage(request);
    }

    @Override
    public Optional<FileObject> findById(Long fileId) {
        return Optional.ofNullable(fileObjectMapper.selectById(fileId));
    }

    @Override
    public int markDeleted(Long fileId, String status) {
        return fileObjectMapper.markDeleted(fileId, status);
    }
}
