package com.xd.smartworksite.file.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.file.domain.FileObject;
import com.xd.smartworksite.file.domain.FileParseRecord;
import com.xd.smartworksite.file.infra.DocumentParseModelAdapter;
import com.xd.smartworksite.file.infra.DocumentPreparationService;
import com.xd.smartworksite.file.infra.ParsedDocument;
import com.xd.smartworksite.file.infra.PreparedDocument;
import com.xd.smartworksite.file.infra.StorageAdapter;
import com.xd.smartworksite.file.repository.FileObjectRepository;
import com.xd.smartworksite.file.repository.FileParseRecordRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileParseWorkerTest {

    @Test
    void parsesLongTextInBatchesWithoutTruncation() {
        FileObjectRepository fileRepository = mock(FileObjectRepository.class);
        FileParseRecordRepository parseRepository = mock(FileParseRecordRepository.class);
        DocumentPreparationService preparationService = mock(DocumentPreparationService.class);
        DocumentParseModelAdapter modelAdapter = mock(DocumentParseModelAdapter.class);
        StorageAdapter storageAdapter = mock(StorageAdapter.class);
        FileProperties properties = new FileProperties();
        properties.getParse().setMaxInputChars(1000);
        ObjectMapper objectMapper = new ObjectMapper();
        FileParseWorker worker = new FileParseWorker(
                fileRepository, parseRepository, preparationService, modelAdapter,
                storageAdapter, properties, objectMapper);

        FileParseRecord record = new FileParseRecord();
        record.setId(9L);
        record.setProjectId(1L);
        record.setFileId(2L);
        record.setResultFormat("MARKDOWN");
        FileObject file = new FileObject();
        file.setId(2L);
        file.setProjectId(1L);
        file.setFileName("long.txt");
        file.setContentType("text/plain");
        String source = "安全检查内容\n".repeat(400);
        when(parseRepository.findById(9L)).thenReturn(Optional.of(record));
        when(fileRepository.findById(2L)).thenReturn(Optional.of(file));
        when(parseRepository.updateRunning(anyLong(), anyString(), anyInt())).thenReturn(1);
        when(parseRepository.updateSucceeded(any())).thenReturn(1);
        when(preparationService.prepare(file)).thenReturn(PreparedDocument.text("txt", source, 0, false));
        when(modelAdapter.parse(any())).thenAnswer(invocation -> {
            var request = (com.xd.smartworksite.file.infra.DocumentParseRequest) invocation.getArgument(0);
            return new ParsedDocument(
                    request.getTextContent(), "MARKDOWN", "model-test", "{\"part\":true}");
        });

        worker.parseAsync(9L);

        verify(modelAdapter, times(3)).parse(any());
        ArgumentCaptor<FileParseRecord> success = ArgumentCaptor.forClass(FileParseRecord.class);
        verify(parseRepository).updateSucceeded(success.capture());
        assertThat(success.getValue().getMetadata()).contains("\"inputTruncated\":false", "\"batchCount\":3");
        verify(storageAdapter).upload(any(), any(), any(Long.class), any());
    }
}
