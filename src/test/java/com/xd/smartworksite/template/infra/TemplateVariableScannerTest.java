package com.xd.smartworksite.template.infra;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateVariableScannerTest {

    private final TemplateVariableScanner scanner = new TemplateVariableScanner();

    @Test
    void scansDocxInDocumentOrderAndRecognizesVariablesSplitAcrossRuns() throws Exception {
        byte[] documentBytes;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("项目：{{ var_");
            paragraph.createRun().setText("project_name }}");
            XWPFTable table = document.createTable(1, 1);
            table.getRow(0).getCell(0).setText("日期：{{var_report_date}}");
            document.createParagraph().createRun().setText("重复：{{ var_project_name }}");
            document.write(outputStream);
            documentBytes = outputStream.toByteArray();
        }

        assertThat(scanner.scan("template.docx", new ByteArrayInputStream(documentBytes)))
                .containsExactly("var_project_name", "var_report_date");
    }

    @Test
    void scansExcelBySheetRowAndCellOrder() throws Exception {
        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Row firstRow = workbook.createSheet("第一页").createRow(0);
            firstRow.createCell(0).setCellValue("{{ var_first }}");
            firstRow.createCell(1).setCellValue("{{var_second}}");
            workbook.createSheet("第二页").createRow(0).createCell(0).setCellValue("{{ var_third }}");
            workbook.write(outputStream);
            workbookBytes = outputStream.toByteArray();
        }

        assertThat(scanner.scan("template.xlsx", new ByteArrayInputStream(workbookBytes)))
                .containsExactly("var_first", "var_second", "var_third");
    }

    @Test
    void scansOnlyNormalizedVarPlaceholdersAndDeduplicatesThem() throws Exception {
        String content = "{{ var_one }} ${var_old} {{ name }} {{ VAR_UPPER }} {{var_one}} {{var_two}}";

        assertThat(scanner.scan("template.md", new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))))
                .containsExactly("var_one", "var_two");
    }

    @Test
    void scansReviewDescriptionsAndSplitsOnlyOnFirstColon() throws Exception {
        String content = """
                {{var_project_name:检查项目名称是否一致}}
                {{ var_emergency_plan : 审核要求：必须包含联系人、流程和物资 }}
                {{var_project_name:检查项目名称是否一致}}
                """;

        assertThat(scanner.scanReviewDescriptions(
                "review-template.md",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        )).containsExactly(
                java.util.Map.entry("var_project_name", "检查项目名称是否一致"),
                java.util.Map.entry("var_emergency_plan", "审核要求：必须包含联系人、流程和物资")
        );
    }

    @Test
    void scansReviewDescriptionSplitAcrossWordRuns() throws Exception {
        byte[] documentBytes;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("{{ var_safety_");
            paragraph.createRun().setText("measure : 检查安全措施");
            paragraph.createRun().setText("是否完整 }}");
            document.write(outputStream);
            documentBytes = outputStream.toByteArray();
        }

        assertThat(scanner.scanReviewDescriptions(
                "review-template.docx",
                new ByteArrayInputStream(documentBytes)
        )).containsExactly(java.util.Map.entry("var_safety_measure", "检查安全措施是否完整"));
    }

    @Test
    void rejectsReviewVariableWithoutDescriptionOrWithConflictingDuplicate() {
        assertThatThrownBy(() -> scanner.scanReviewDescriptions(
                "review-template.md",
                new ByteArrayInputStream("{{var_project_name:}}".getBytes(StandardCharsets.UTF_8))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("描述不能为空");

        assertThatThrownBy(() -> scanner.scanReviewDescriptions(
                "review-template.md",
                new ByteArrayInputStream(
                        "{{var_project_name:规则一}}{{var_project_name:规则二}}".getBytes(StandardCharsets.UTF_8)
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("存在不同描述");
    }

    @Test
    void rejectsPdfTemplates() {
        assertThatThrownBy(() -> scanner.scan("template.pdf", new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported template format");
    }
}
