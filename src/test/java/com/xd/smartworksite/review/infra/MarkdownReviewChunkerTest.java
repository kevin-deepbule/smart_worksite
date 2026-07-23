package com.xd.smartworksite.review.infra;

import com.xd.smartworksite.review.application.ReviewProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReviewChunkerTest {

    @Test
    void preservesSheetAndRowLocationWhenChunkingExcelMarkdown() {
        ReviewProperties properties = new ReviewProperties();
        properties.getChunk().setMaxChars(1000);
        MarkdownReviewChunker chunker = new MarkdownReviewChunker(properties);

        var chunks = chunker.split("""
                # Sheet: 检查清单

                | 行号 | 列1 | 列2 |
                | --- | --- | --- |
                | 1 | 检查项 | 结果 |
                | 2 | 临边防护 | 符合 |
                """);

        assertThat(chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.chunkCode()).isEqualTo("CHUNK_0001");
            assertThat(chunk.sheetName()).isEqualTo("检查清单");
            assertThat(chunk.rowStart()).isEqualTo(1);
            assertThat(chunk.rowEnd()).isEqualTo(2);
            assertThat(chunk.contentHash()).hasSize(64);
        });
    }

    @Test
    void splitsLongMarkdownIntoStableOrderedChunks() {
        ReviewProperties properties = new ReviewProperties();
        properties.getChunk().setMaxChars(1000);
        properties.getChunk().setOverlapChars(100);
        MarkdownReviewChunker chunker = new MarkdownReviewChunker(properties);
        String markdown = "# 第一章\n" + "安全措施说明。".repeat(400);

        var chunks = chunker.split(markdown);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).chunkCode()).isEqualTo("CHUNK_0001");
        assertThat(chunks.get(1).chunkCode()).isEqualTo("CHUNK_0002");
    }

    @Test
    void assignsHeadingAndSheetMetadataToTheCorrectStructuralChunk() {
        ReviewProperties properties = new ReviewProperties();
        properties.getChunk().setMaxChars(1000);
        MarkdownReviewChunker chunker = new MarkdownReviewChunker(properties);

        var chunks = chunker.split("""
                # 第一章
                第一章内容
                # Sheet: 检查清单
                | 行号 | 结果 |
                | --- | --- |
                | 7 | 符合 |
                """);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).headingPath()).isEqualTo("第一章");
        assertThat(chunks.get(0).sheetName()).isNull();
        assertThat(chunks.get(1).headingPath()).isEqualTo("Sheet: 检查清单");
        assertThat(chunks.get(1).sheetName()).isEqualTo("检查清单");
        assertThat(chunks.get(1).rowStart()).isEqualTo(7);
        assertThat(chunks.get(1).rowEnd()).isEqualTo(7);
    }
}
