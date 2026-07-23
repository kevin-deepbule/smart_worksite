package com.xd.smartworksite.template.infra;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateVariableScanner {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(var_[a-z0-9_]+)\\s*\\}\\}");
    private static final Pattern REVIEW_VARIABLE_PATTERN = Pattern.compile(
            "\\{\\{\\s*(var_[a-z0-9_]+)\\s*:(.*?)\\}\\}",
            Pattern.DOTALL
    );
    private static final Pattern REVIEW_VARIABLE_TOKEN_PATTERN = Pattern.compile(
            "\\{\\{\\s*(?i:var)[^{}]*\\}\\}",
            Pattern.DOTALL
    );
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    public List<String> scan(String fileName, InputStream inputStream) throws IOException {
        String extension = TemplateFileSupport.extension(fileName);
        if (!TemplateFileSupport.isSupported(fileName)) {
            throw new IllegalArgumentException("unsupported template format: " + extension);
        }

        Set<String> variables = new LinkedHashSet<>();
        scanContent(extension, inputStream, text -> scanReportText(text, variables));
        return new ArrayList<>(variables);
    }

    public Map<String, String> scanReviewDescriptions(String fileName, InputStream inputStream) throws IOException {
        String extension = TemplateFileSupport.extension(fileName);
        if (!TemplateFileSupport.isSupported(fileName)) {
            throw new IllegalArgumentException("unsupported template format: " + extension);
        }

        Map<String, String> descriptions = new LinkedHashMap<>();
        scanContent(extension, inputStream, text -> scanReviewText(text, descriptions));
        return descriptions;
    }

    private void scanContent(String extension, InputStream inputStream, Consumer<String> textConsumer) throws IOException {
        switch (extension) {
            case "docx" -> scanDocx(inputStream, textConsumer);
            case "doc" -> scanDoc(inputStream, textConsumer);
            case "xls", "xlsx" -> scanWorkbook(inputStream, textConsumer);
            case "csv", "txt", "md" -> textConsumer.accept(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            default -> throw new IllegalArgumentException("unsupported template format: " + extension);
        }
    }

    private void scanDocx(InputStream inputStream, Consumer<String> textConsumer) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            for (XWPFHeader header : document.getHeaderList()) {
                scanBodyElements(header.getBodyElements(), textConsumer);
            }
            scanBodyElements(document.getBodyElements(), textConsumer);
            for (XWPFFooter footer : document.getFooterList()) {
                scanBodyElements(footer.getBodyElements(), textConsumer);
            }
        }
    }

    private void scanBodyElements(List<IBodyElement> elements, Consumer<String> textConsumer) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph paragraph) {
                textConsumer.accept(paragraph.getText());
            } else if (element instanceof XWPFTable table) {
                scanTable(table, textConsumer);
            }
        }
    }

    private void scanTable(XWPFTable table, Consumer<String> textConsumer) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                scanBodyElements(cell.getBodyElements(), textConsumer);
            }
        }
    }

    private void scanDoc(InputStream inputStream, Consumer<String> textConsumer) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            textConsumer.accept(extractor.getText());
        }
    }

    private void scanWorkbook(InputStream inputStream, Consumer<String> textConsumer) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        textConsumer.accept(formatter.formatCellValue(cell, evaluator));
                    }
                }
            }
        }
    }

    private void scanReportText(String text, Set<String> variables) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
    }

    private void scanReviewText(String text, Map<String, String> descriptions) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Matcher tokenMatcher = REVIEW_VARIABLE_TOKEN_PATTERN.matcher(text);
        while (tokenMatcher.find()) {
            String token = tokenMatcher.group();
            Matcher variableMatcher = REVIEW_VARIABLE_PATTERN.matcher(token);
            if (!variableMatcher.matches()) {
                throw new IllegalArgumentException("审查模板变量格式错误: " + token);
            }
            String variableName = variableMatcher.group(1);
            String description = variableMatcher.group(2) == null ? "" : variableMatcher.group(2).trim();
            if (description.isEmpty()) {
                throw new IllegalArgumentException("审查模板变量描述不能为空: " + variableName);
            }
            if (description.length() > MAX_DESCRIPTION_LENGTH) {
                throw new IllegalArgumentException("审查模板变量描述不能超过2000个字符: " + variableName);
            }
            String existing = descriptions.putIfAbsent(variableName, description);
            if (existing != null && !existing.equals(description)) {
                throw new IllegalArgumentException("审查模板变量存在不同描述: " + variableName);
            }
        }
    }
}
