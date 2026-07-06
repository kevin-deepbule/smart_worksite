package com.xd.smartworksite.file.mapper;

import com.xd.smartworksite.file.domain.FileObject;
import com.xd.smartworksite.file.dto.FileQueryRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FileObjectMapper {

    int insert(FileObject fileObject);

    List<FileObject> selectPage(FileQueryRequest request);

    FileObject selectById(@Param("fileId") Long fileId);

    int markDeleted(@Param("fileId") Long fileId, @Param("status") String status);
}
