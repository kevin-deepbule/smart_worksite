package com.xd.smartworksite.file.infra;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.file.application.FileProperties;
import com.xd.smartworksite.file.domain.FileObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentPreparationServiceTest {

    @Test
    void preparesUtf8TextWithoutLosingContent() {
        PreparedDocument result = service("安全检查\n临边防护".getBytes()).prepare(
                file("manual.txt", "txt", "text/plain"));

        assertThat(result.getInputFormat()).isEqualTo("txt");
        assertThat(result.getTextContent()).contains("安全检查", "临边防护");
        assertThat(result.isTruncated()).isFalse();
    }

    @Test
    void rejectsMalformedUtf8Text() {
        byte[] malformed = {(byte) 0xC3, (byte) 0x28};

        assertThatThrownBy(() -> service(malformed).prepare(file("bad.txt", "txt", "text/plain")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("UTF-8");
    }

    @Test
    void preparesExcelWithSheetNameAndSourceRowNumbers() throws Exception {
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("检查清单");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("检查项");
            header.createCell(1).setCellValue("结果");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("临边防护");
            row.createCell(1).setCellValue("符合");
            workbook.write(output);
            bytes = output.toByteArray();
        }

        PreparedDocument result = service(bytes).prepare(file(
                "check.xlsx", "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        assertThat(result.getInputFormat()).isEqualTo("xlsx");
        assertThat(result.getTextContent())
                .contains("# Sheet: 检查清单")
                .contains("| 1 | 检查项 | 结果 |")
                .contains("| 2 | 临边防护 | 符合 |");
    }

    @Test
    void rejectsTextBeyondTheConfiguredWholeDocumentLimitInsteadOfTruncatingIt() {
        FileProperties properties = new FileProperties();
        properties.getParse().setMaxDocumentChars(10);
        DocumentPreparationService service =
                new DocumentPreparationService(new BytesStorageAdapter("12345678901".getBytes()), properties);

        assertThatThrownBy(() -> service.prepare(file("long.txt", "txt", "text/plain")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("parse size limit");
    }

    @Test
    void preparesPdfWithStablePageAnchors() throws Exception {
        byte[] bytes;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int pageNumber = 1; pageNumber <= 2; pageNumber++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 12);
                    content.newLineAtOffset(72, 720);
                    content.showText("page " + pageNumber + " content");
                    content.endText();
                }
            }
            document.save(output);
            bytes = output.toByteArray();
        }

        PreparedDocument result =
                service(bytes).prepare(file("manual.pdf", "pdf", "application/pdf"));

        assertThat(result.getTextContent())
                .contains("## Page: 1", "page 1 content", "## Page: 2", "page 2 content");
        assertThat(result.getPageCount()).isEqualTo(2);
    }

    private DocumentPreparationService service(byte[] bytes) {
        FileProperties properties = new FileProperties();
        properties.getParse().setMaxInputChars(120000);
        return new DocumentPreparationService(new BytesStorageAdapter(bytes), properties);
    }

    private FileObject file(String name, String extension, String contentType) {
        FileObject file = new FileObject();
        file.setId(1L);
        file.setProjectId(1L);
        file.setFileName(name);
        file.setFileExt(extension);
        file.setContentType(contentType);
        file.setObjectName("source");
        return file;
    }

    private record BytesStorageAdapter(byte[] bytes) implements StorageAdapter {
        @Override
        public StorageObject upload(String objectName, InputStream inputStream, long size, String contentType) {
            return new StorageObject(objectName, "test", contentType, size);
        }

        @Override
        public InputStream openObject(String objectName) {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public String createAccessUrl(String objectName, Duration expire) {
            return "";
        }

        @Override
        public void delete(String objectName) {
        }
    }
}
