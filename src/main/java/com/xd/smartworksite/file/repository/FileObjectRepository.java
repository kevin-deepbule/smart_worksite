package com.xd.smartworksite.file.repository;

import com.xd.smartworksite.file.domain.FileObject;
import com.xd.smartworksite.file.dto.FileQueryRequest;

import java.util.List;
import java.util.Optional;

public interface FileObjectRepository {

    void insert(FileObject fileObject);

    List<FileObject> findPage(FileQueryRequest request);

    Optional<FileObject> findById(Long fileId);

    int markDeleted(Long fileId, String status);
}
