package com.xd.smartworksite.file.infra;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.file.application.FileProperties;
import com.xd.smartworksite.file.domain.FileObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

@Service
public class DocumentPreparationService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final StorageAdapter storageAdapter;
    private final FileProperties fileProperties;

    public DocumentPreparationService(StorageAdapter storageAdapter, FileProperties fileProperties) {
        this.storageAdapter = storageAdapter;
        this.fileProperties = fileProperties;
    }

    public PreparedDocument prepare(FileObject fileObject) {
        String contentType = normalizeContentType(fileObject.getContentType());
        String fileExt = normalizeExt(fileObject.getFileExt());
        try (InputStream inputStream = storageAdapter.openObject(fileObject.getObjectName())) {
            byte[] bytes = readAll(inputStream);
            if (IMAGE_TYPES.contains(contentType)) {
                return PreparedDocument.image(fileExt, "data:" + contentType + ";base64,"
                        + Base64.getEncoder().encodeToString(bytes));
            }
            if ("application/pdf".equals(contentType) || "pdf".equals(fileExt)) {
                return preparePdf(bytes);
            }
            if ("docx".equals(fileExt) || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
                return prepareDocx(bytes);
            }
            if ("doc".equals(fileExt) || "application/msword".equals(contentType)) {
                return prepareDoc(bytes);
            }
            if ("xlsx".equals(fileExt)
                    || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType)) {
                return prepareExcel(bytes, "xlsx");
            }
            if ("xls".equals(fileExt) || "application/vnd.ms-excel".equals(contentType)) {
                return prepareExcel(bytes, "xls");
            }
            if ("txt".equals(fileExt) || "md".equals(fileExt) || "text/plain".equals(contentType)) {
                return prepareText(bytes, fileExt.isBlank() ? "txt" : fileExt);
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported file parse content type");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "prepare file parse input failed");
        }
    }

    private PreparedDocument preparePdf(byte[] bytes) throws Exception {
        try (PDDocument document = PDDocument.load(bytes)) {
            int pageCount = document.getNumberOfPages();
            if (pageCount > fileProperties.getParse().getMaxPages()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "pdf page count exceeds parse limit");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                text.append("## Page: ").append(page).append('\n')
                        .append(stripper.getText(document).trim())
                        .append("\n\n");
            }
            return preparedText("pdf", text.toString(), pageCount);
        }
    }

    private PreparedDocument prepareDocx(byte[] bytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return preparedText("docx", extractor.getText(), 0);
        }
    }

    private PreparedDocument prepareDoc(byte[] bytes) throws Exception {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            return preparedText("doc", extractor.getText(), 0);
        }
    }

    private PreparedDocument prepareText(byte[] bytes, String inputFormat) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return preparedText(inputFormat, text, 0);
        } catch (CharacterCodingException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "text file must use valid UTF-8 encoding");
        }
    }

    private PreparedDocument prepareExcel(byte[] bytes, String inputFormat) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            StringBuilder markdown = new StringBuilder();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                markdown.append("# Sheet: ").append(escapeMarkdown(sheet.getSheetName())).append("\n\n");
                int maxColumn = maxColumnCount(sheet);
                if (maxColumn == 0) {
                    markdown.append("_空工作表_\n\n");
                    continue;
                }
                markdown.append("| 行号 |");
                for (int column = 0; column < maxColumn; column++) {
                    markdown.append(" 列").append(column + 1).append(" |");
                }
                markdown.append("\n| --- |");
                for (int column = 0; column < maxColumn; column++) {
                    markdown.append(" --- |");
                }
                markdown.append('\n');
                for (Row row : sheet) {
                    markdown.append("| ").append(row.getRowNum() + 1).append(" |");
                    for (int column = 0; column < maxColumn; column++) {
                        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        String value = cell == null ? "" : formatter.formatCellValue(cell);
                        markdown.append(' ').append(escapeMarkdown(value)).append(" |");
                    }
                    markdown.append('\n');
                }
                markdown.append('\n');
            }
            return preparedText(inputFormat, markdown.toString(), 0);
        }
    }

    private int maxColumnCount(Sheet sheet) {
        int maxColumn = 0;
        for (Row row : sheet) {
            maxColumn = Math.max(maxColumn, Math.max(0, row.getLastCellNum()));
        }
        return maxColumn;
    }

    private String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\r", " ").replace("\n", "<br>");
    }

    private PreparedDocument preparedText(String inputFormat, String text, int pageCount) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "document text is empty or unsupported for parsing");
        }
        int maxDocumentChars = Math.max(1, fileProperties.getParse().getMaxDocumentChars());
        if (text.length() > maxDocumentChars) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "document text exceeds parse size limit");
        }
        return PreparedDocument.text(inputFormat, text, pageCount, false);
    }

    private byte[] readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toByteArray();
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeExt(String fileExt) {
        if (fileExt == null || fileExt.isBlank()) {
            return "";
        }
        return fileExt.trim().toLowerCase(Locale.ROOT);
    }
}
