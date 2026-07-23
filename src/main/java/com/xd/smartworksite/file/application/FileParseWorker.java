package com.xd.smartworksite.file.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.file.domain.FileObject;
import com.xd.smartworksite.file.domain.FileParseRecord;
import com.xd.smartworksite.file.domain.FileParseStage;
import com.xd.smartworksite.file.domain.FileParseStatus;
import com.xd.smartworksite.file.infra.DocumentParseModelAdapter;
import com.xd.smartworksite.file.infra.DocumentParseRequest;
import com.xd.smartworksite.file.infra.DocumentPreparationService;
import com.xd.smartworksite.file.infra.ParsedDocument;
import com.xd.smartworksite.file.infra.PreparedDocument;
import com.xd.smartworksite.file.infra.StorageAdapter;
import com.xd.smartworksite.file.repository.FileObjectRepository;
import com.xd.smartworksite.file.repository.FileParseRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FileParseWorker {

    private static final Logger log = LoggerFactory.getLogger(FileParseWorker.class);

    private final FileObjectRepository fileObjectRepository;
    private final FileParseRecordRepository fileParseRecordRepository;
    private final DocumentPreparationService documentPreparationService;
    private final DocumentParseModelAdapter documentParseModelAdapter;
    private final StorageAdapter storageAdapter;
    private final FileProperties fileProperties;
    private final ObjectMapper objectMapper;

    public FileParseWorker(FileObjectRepository fileObjectRepository,
                           FileParseRecordRepository fileParseRecordRepository,
                           DocumentPreparationService documentPreparationService,
                           DocumentParseModelAdapter documentParseModelAdapter,
                           StorageAdapter storageAdapter,
                           FileProperties fileProperties,
                           ObjectMapper objectMapper) {
        this.fileObjectRepository = fileObjectRepository;
        this.fileParseRecordRepository = fileParseRecordRepository;
        this.documentPreparationService = documentPreparationService;
        this.documentParseModelAdapter = documentParseModelAdapter;
        this.storageAdapter = storageAdapter;
        this.fileProperties = fileProperties;
        this.objectMapper = objectMapper;
    }

    @Async("fileParseTaskExecutor")
    public void parseAsync(Long recordId) {
        try {
            FileParseRecord record = fileParseRecordRepository.findById(recordId).orElseThrow();
            FileObject fileObject = fileObjectRepository.findById(record.getFileId()).orElseThrow();

            update(recordId, FileParseStage.LOADING_SOURCE, 10);
            update(recordId, FileParseStage.PREPARING_INPUT, 25);
            PreparedDocument preparedDocument = documentPreparationService.prepare(fileObject);

            update(recordId, FileParseStage.CALLING_MODEL, 55);
            ParsedDocument parsedDocument = parsePreparedDocument(record, fileObject, preparedDocument);

            update(recordId, FileParseStage.NORMALIZING_RESULT, 80);
            String resultContent = parsedDocument.getContent().trim();
            String resultObjectName = buildResultObjectName(record, parsedDocument.getResultFormat());

            update(recordId, FileParseStage.STORING_RESULT, 90);
            byte[] resultBytes = resultContent.getBytes(StandardCharsets.UTF_8);
            storageAdapter.upload(
                    resultObjectName,
                    new ByteArrayInputStream(resultBytes),
                    resultBytes.length,
                    "MARKDOWN".equals(parsedDocument.getResultFormat()) ? "text/markdown; charset=utf-8" : "text/plain; charset=utf-8"
            );

            FileParseRecord success = new FileParseRecord();
            success.setId(recordId);
            success.setCurrentStage(FileParseStage.FINISHED.name());
            success.setResultObjectName(resultObjectName);
            success.setContentPreview(toPreview(resultContent));
            success.setMetadata(buildMetadata(parsedDocument, preparedDocument));
            requireUpdated(fileParseRecordRepository.updateSucceeded(success),
                    "file parse success state changed");
        } catch (Exception ex) {
            log.warn("file parse failed, recordId={}", recordId, ex);
            int failed = fileParseRecordRepository.updateFailed(
                    recordId,
                    FileParseStage.FAILED.name(),
                    normalizeErrorMessage(ex)
            );
            if (failed == 0) {
                throw new IllegalStateException(
                        "file parse failure state cannot be persisted: " + normalizeErrorMessage(ex), ex);
            }
        }
    }

    private void update(Long recordId, FileParseStage stage, int progress) {
        requireUpdated(fileParseRecordRepository.updateRunning(recordId, stage.name(), progress),
                "file parse stage state changed");
    }

    private DocumentParseRequest toModelRequest(FileParseRecord record, FileObject fileObject, PreparedDocument preparedDocument) {
        return toModelRequest(record, fileObject, preparedDocument, preparedDocument.getTextContent(), 1, 1);
    }

    private DocumentParseRequest toModelRequest(FileParseRecord record, FileObject fileObject,
                                                PreparedDocument preparedDocument, String textContent,
                                                int partIndex, int partCount) {
        DocumentParseRequest request = new DocumentParseRequest();
        request.setProjectId(record.getProjectId());
        request.setFileId(record.getFileId());
        request.setRecordId(record.getId());
        request.setFileName(fileObject.getFileName());
        request.setContentType(fileObject.getContentType());
        request.setInputFormat(preparedDocument.getInputFormat());
        request.setTargetFormat(record.getResultFormat());
        request.setLanguage("zh-CN");
        request.setTextContent(textContent);
        request.setImageDataUrl(preparedDocument.getImageDataUrl());
        request.setPartIndex(partIndex);
        request.setPartCount(partCount);
        return request;
    }

    private ParsedDocument parsePreparedDocument(FileParseRecord record, FileObject fileObject,
                                                 PreparedDocument preparedDocument) throws Exception {
        String text = preparedDocument.getTextContent();
        if (text == null) {
            return documentParseModelAdapter.parse(toModelRequest(record, fileObject, preparedDocument));
        }
        List<String> parts = splitText(text, fileProperties.getParse().getMaxInputChars());
        if (parts.size() == 1) {
            return documentParseModelAdapter.parse(toModelRequest(record, fileObject, preparedDocument));
        }

        StringBuilder markdown = new StringBuilder();
        List<Object> modelMetadata = new ArrayList<>();
        String resultFormat = null;
        String modelName = null;
        for (int index = 0; index < parts.size(); index++) {
            ParsedDocument part = documentParseModelAdapter.parse(toModelRequest(
                    record, fileObject, preparedDocument, parts.get(index), index + 1, parts.size()));
            if (resultFormat == null) {
                resultFormat = part.getResultFormat();
                modelName = part.getModelName();
            } else if (!resultFormat.equals(part.getResultFormat())) {
                throw new IllegalStateException("document parse batch result format mismatch");
            }
            if (markdown.length() > 0) {
                markdown.append("\n\n");
            }
            markdown.append("<!-- source-part: ")
                    .append(index + 1).append('/').append(parts.size()).append(" -->\n")
                    .append(part.getContent().trim());
            if (part.getMetadata() != null && !part.getMetadata().isBlank()) {
                modelMetadata.add(objectMapper.readTree(part.getMetadata()));
            }
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("batchCount", parts.size());
        metadata.put("batches", modelMetadata);
        return new ParsedDocument(markdown.toString(), resultFormat, modelName,
                objectMapper.writeValueAsString(metadata));
    }

    private List<String> splitText(String text, int configuredMaxChars) {
        int maxChars = Math.max(1000, configuredMaxChars);
        if (text.length() <= maxChars) {
            return List.of(text);
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + maxChars);
            if (end < text.length()) {
                int newline = text.lastIndexOf('\n', end);
                if (newline > start) {
                    end = newline + 1;
                }
            }
            parts.add(text.substring(start, end));
            start = end;
        }
        return parts;
    }

    private String buildResultObjectName(FileParseRecord record, String resultFormat) {
        LocalDate today = LocalDate.now();
        String suffix = "MARKDOWN".equals(resultFormat) ? ".md" : ".txt";
        return "projects/%d/PARSE_RESULT/%04d/%02d/%02d/%d-%d%s".formatted(
                record.getProjectId(),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                record.getFileId(),
                record.getId(),
                suffix
        );
    }

    private String toPreview(String content) {
        int maxLength = fileProperties.getParse().getResultPreviewLength();
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength);
    }

    private String buildMetadata(ParsedDocument parsedDocument, PreparedDocument preparedDocument) throws Exception {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("provider", "QWEN_VL");
        metadata.put("model", parsedDocument.getModelName());
        metadata.put("pageCount", preparedDocument.getPageCount());
        metadata.put("inputTruncated", preparedDocument.isTruncated());
        if (parsedDocument.getMetadata() != null && !parsedDocument.getMetadata().isBlank()) {
            metadata.put("modelMetadata", objectMapper.readTree(parsedDocument.getMetadata()));
        }
        return objectMapper.writeValueAsString(metadata);
    }

    private String normalizeErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private void requireUpdated(int updated, String message) {
        if (updated == 0) {
            throw new IllegalStateException(message);
        }
    }
}
