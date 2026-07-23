package com.xd.smartworksite.review.infra;

public record MarkdownReviewChunk(
        int chunkNo,
        String chunkCode,
        String content,
        String headingPath,
        Integer pageStart,
        Integer pageEnd,
        String sheetName,
        Integer rowStart,
        Integer rowEnd,
        String contentHash,
        int tokenCount
) {
}
