package com.xd.smartworksite.file.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.common.result.PageResult;
import com.xd.smartworksite.file.domain.FileBizType;
import com.xd.smartworksite.file.domain.FileObject;
import com.xd.smartworksite.file.domain.FileStatus;
import com.xd.smartworksite.file.dto.FileAccessUrlResponse;
import com.xd.smartworksite.file.dto.FileObjectResponse;
import com.xd.smartworksite.file.dto.FileQueryRequest;
import com.xd.smartworksite.file.dto.FileUploadRequest;
import com.xd.smartworksite.file.infra.StorageAdapter;
import com.xd.smartworksite.file.infra.StorageObject;
import com.xd.smartworksite.file.repository.FileObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(FileProperties.class)
public class FileObjectApplicationService {

    private static final Logger log = LoggerFactory.getLogger(FileObjectApplicationService.class);
    private static final Set<String> PREVIEW_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "application/pdf",
            "text/plain"
    );

    private final FileObjectRepository fileObjectRepository;
    private final StorageAdapter storageAdapter;
    private final FileProperties fileProperties;
    private final ObjectMapper objectMapper;

    public FileObjectApplicationService(FileObjectRepository fileObjectRepository,
                                        StorageAdapter storageAdapter,
                                        FileProperties fileProperties,
                                        ObjectMapper objectMapper) {
        this.fileObjectRepository = fileObjectRepository;
        this.storageAdapter = storageAdapter;
        this.fileProperties = fileProperties;
        this.objectMapper = objectMapper;
    }

    public FileObjectResponse upload(FileUploadRequest request) {
        MultipartFile file = request.getFile();
        validateFile(file);
        FileBizType bizType = parseBizType(request.getBizType());
        String originalFilename = normalizeFilename(file.getOriginalFilename());
        String fileExt = extractFileExt(originalFilename);
        String contentType = normalizeContentType(file.getContentType());
        validateContentType(contentType);
        String metadata = normalizeMetadata(request.getMetadata());
        String fileHash = calculateSha256(file);
        String objectName = buildObjectName(request.getProjectId(), bizType, fileExt);

        StorageObject storageObject;
        try (InputStream inputStream = file.getInputStream()) {
            storageObject = storageAdapter.upload(objectName, inputStream, file.getSize(), contentType);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "upload file object failed");
        }

        FileObject fileObject = new FileObject();
        fileObject.setProjectId(request.getProjectId());
        fileObject.setBizType(bizType.name());
        fileObject.setBizId(request.getBizId());
        fileObject.setFileName(originalFilename);
        fileObject.setFileExt(fileExt);
        fileObject.setObjectName(storageObject.getObjectName());
        fileObject.setStorageBucket(storageObject.getBucket());
        fileObject.setContentType(storageObject.getContentType());
        fileObject.setFileSize(storageObject.getSize());
        fileObject.setFileHash(fileHash);
        fileObject.setStatus(FileStatus.ACTIVE.name());
        fileObject.setMetadata(metadata);
        fileObject.setPreviewSupported(isPreviewSupported(contentType));

        try {
            fileObjectRepository.insert(fileObject);
        } catch (RuntimeException ex) {
            cleanupUploadedObject(storageObject.getObjectName());
            throw ex;
        }

        return fileObjectRepository.findById(fileObject.getId())
                .map(this::toResponse)
                .orElseGet(() -> toResponse(fileObject));
    }

    public PageResult<FileObjectResponse> queryFiles(FileQueryRequest request) {
        normalizeQuery(request);
        // TODO: verify current user has access to request.getProjectId() before file operation.
        Page<FileObject> page = PageHelper.startPage(request.getPageNo(), request.getPageSize())
                .doSelectPage(() -> fileObjectRepository.findPage(request));
        return new PageResult<>(
                request.getPageNo(),
                request.getPageSize(),
                page.getTotal(),
                page.getResult().stream().map(this::toResponse).toList()
        );
    }

    public FileObjectResponse getFile(Long fileId) {
        FileObject fileObject = findActiveFile(fileId);
        // TODO: verify current user has access to fileObject.getProjectId() before file operation.
        return toResponse(fileObject);
    }

    public FileAccessUrlResponse createDownloadUrl(Long fileId) {
        FileObject fileObject = findActiveFile(fileId);
        // TODO: verify current user has access to fileObject.getProjectId() before file operation.
        return createAccessUrl(fileObject, true);
    }

    public FileAccessUrlResponse createPreviewUrl(Long fileId) {
        FileObject fileObject = findActiveFile(fileId);
        // TODO: verify current user has access to fileObject.getProjectId() before file operation.
        if (!Boolean.TRUE.equals(fileObject.getPreviewSupported())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "file preview is not supported");
        }
        return createAccessUrl(fileObject, true);
    }

    public void deleteFile(Long fileId) {
        FileObject fileObject = findActiveFile(fileId);
        // TODO: verify current user has access to fileObject.getProjectId() before file operation.
        try {
            storageAdapter.delete(fileObject.getObjectName());
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "delete file object failed");
        }
        int updated = fileObjectRepository.markDeleted(fileId, FileStatus.DELETED.name());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "file not found");
        }
    }

    private FileObject findActiveFile(Long fileId) {
        return fileObjectRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "file not found"));
    }

    private FileAccessUrlResponse createAccessUrl(FileObject fileObject, boolean previewSupported) {
        long expireSeconds = fileProperties.getAccessUrlExpireSeconds();
        String url;
        try {
            url = storageAdapter.createAccessUrl(fileObject.getObjectName(), Duration.ofSeconds(expireSeconds));
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "create file access url failed");
        }

        FileAccessUrlResponse response = new FileAccessUrlResponse();
        response.setFileId(fileObject.getId());
        response.setUrl(url);
        response.setExpiresAt(LocalDateTime.now().plusSeconds(expireSeconds));
        response.setPreviewSupported(previewSupported && Boolean.TRUE.equals(fileObject.getPreviewSupported()));
        return response;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "file is required");
        }
        if (file.getSize() > fileProperties.getMaxSizeBytes()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "file size exceeds limit");
        }
    }

    private FileBizType parseBizType(String value) {
        try {
            return FileBizType.from(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid file business type");
        }
    }

    private FileStatus parseStatus(String value) {
        try {
            return FileStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid file status");
        }
    }

    private void normalizeQuery(FileQueryRequest request) {
        if (request.getBizType() != null && !request.getBizType().isBlank()) {
            request.setBizType(parseBizType(request.getBizType()).name());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            request.setStatus(parseStatus(request.getStatus()).name());
        }
    }

    private void validateContentType(String contentType) {
        if (fileProperties.getAllowedContentTypes() == null || fileProperties.getAllowedContentTypes().isEmpty()) {
            return;
        }
        Set<String> allowedTypes = fileProperties.getAllowedContentTypes().stream()
                .map(this::normalizeContentType)
                .collect(Collectors.toSet());
        if (!allowedTypes.contains(contentType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported file content type");
        }
    }

    private String normalizeMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(metadata));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "metadata must be valid json");
        }
    }

    private String calculateSha256(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "calculate file hash failed");
        }
    }

    private String buildObjectName(Long projectId, FileBizType bizType, String fileExt) {
        LocalDate today = LocalDate.now();
        String suffix = fileExt == null || fileExt.isBlank() ? "" : "." + fileExt;
        return "projects/%d/%s/%04d/%02d/%02d/%s%s".formatted(
                projectId,
                bizType.name(),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                suffix
        );
    }

    private String normalizeFilename(String originalFilename) {
        String filename = originalFilename == null || originalFilename.isBlank() ? "file" : originalFilename.trim();
        filename = filename.replace('\\', '/');
        int lastSeparator = filename.lastIndexOf('/');
        if (lastSeparator >= 0) {
            filename = filename.substring(lastSeparator + 1);
        }
        if (filename.length() > 255) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "file name is too long");
        }
        return filename;
    }

    private String extractFileExt(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            return null;
        }
        String fileExt = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (fileExt.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "file extension is too long");
        }
        return fileExt;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private boolean isPreviewSupported(String contentType) {
        return PREVIEW_CONTENT_TYPES.contains(contentType);
    }

    private FileObjectResponse toResponse(FileObject fileObject) {
        FileObjectResponse response = new FileObjectResponse();
        response.setFileId(fileObject.getId());
        response.setProjectId(fileObject.getProjectId());
        response.setBizType(fileObject.getBizType());
        response.setBizId(fileObject.getBizId());
        response.setFileName(fileObject.getFileName());
        response.setFileExt(fileObject.getFileExt());
        response.setContentType(fileObject.getContentType());
        response.setFileSize(fileObject.getFileSize());
        response.setFileHash(fileObject.getFileHash());
        response.setStatus(fileObject.getStatus());
        response.setMetadata(fileObject.getMetadata());
        response.setPreviewSupported(fileObject.getPreviewSupported());
        response.setCreatedAt(fileObject.getCreatedAt());
        response.setUpdatedAt(fileObject.getUpdatedAt());
        return response;
    }

    private void cleanupUploadedObject(String objectName) {
        try {
            storageAdapter.delete(objectName);
        } catch (RuntimeException cleanupEx) {
            log.warn("cleanup uploaded object failed, objectName={}", objectName, cleanupEx);
        }
    }
}
